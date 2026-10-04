package de.jcss.hbt.application;

import de.jcss.hbt.application.port.in.BudgetUseCases.AddExpenseCommand;
import de.jcss.hbt.application.port.in.BudgetUseCases.AddIncomeCommand;
import de.jcss.hbt.application.port.out.BudgetItemRepository;
import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.ExpenseCategory;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link BudgetService} orchestrates the outbound port correctly, using a
 * simple in-memory fake instead of a database.
 */
class BudgetServiceTest {

    private static final LocalDate OCTOBER_4 = LocalDate.of(2026, 10, 4);

    private final FakeRepository repository = new FakeRepository();
    private final BudgetService service = new BudgetService(repository);

    @Test
    void addIncomeStoresItem() {
        BudgetItemId id = service.addIncome(new AddIncomeCommand("Gehalt", new BigDecimal("2000.00"), OCTOBER_4));

        assertThat(repository.items).containsKey(id);
    }

    @Test
    void addExpenseStoresItem() {
        BudgetItemId id = service.addExpense(
                new AddExpenseCommand("Miete", new BigDecimal("800.00"), ExpenseCategory.LEBENSUNTERHALT, OCTOBER_4));

        assertThat(repository.items.get(id).category()).contains(ExpenseCategory.LEBENSUNTERHALT);
    }

    @Test
    void missingAmountIsADomainError() {
        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand("Gehalt", null, OCTOBER_4)))
                .isInstanceOf(InvalidBudgetItemException.class);
        assertThat(repository.items).isEmpty();
    }

    @Test
    void removeItemDeletesIt() {
        BudgetItemId id = service.addIncome(new AddIncomeCommand("Gehalt", new BigDecimal("2000.00"), OCTOBER_4));

        service.removeItem(id);

        assertThat(repository.items).isEmpty();
    }

    @Test
    void removingUnknownItemThrows() {
        assertThatThrownBy(() -> service.removeItem(BudgetItemId.generate()))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    @Test
    void listsAndSummarizesOnlyTheRequestedMonth() {
        service.addIncome(new AddIncomeCommand("Gehalt Oktober", new BigDecimal("2000.00"), OCTOBER_4));
        service.addIncome(new AddIncomeCommand("Gehalt November", new BigDecimal("3000.00"), LocalDate.of(2026, 11, 1)));

        assertThat(service.listItems(YearMonth.of(2026, 10)))
                .extracting(BudgetItem::description)
                .containsExactly("Gehalt Oktober");
        assertThat(service.getOverview(YearMonth.of(2026, 10)).totalIncome().value()).isEqualByComparingTo("2000.00");
    }

    private static final class FakeRepository implements BudgetItemRepository {

        private final Map<BudgetItemId, BudgetItem> items = new LinkedHashMap<>();

        @Override
        public void add(BudgetItem item) {
            items.put(item.id(), item);
        }

        @Override
        public boolean remove(BudgetItemId id) {
            return items.remove(id) != null;
        }

        @Override
        public List<BudgetItem> findByMonth(YearMonth month) {
            return items.values().stream()
                    .filter(item -> item.isBookedIn(month))
                    .sorted(Comparator.comparing(BudgetItem::bookingDate).reversed())
                    .toList();
        }
    }
}
