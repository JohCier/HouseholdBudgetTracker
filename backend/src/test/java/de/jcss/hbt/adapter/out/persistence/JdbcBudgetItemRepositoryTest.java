package de.jcss.hbt.adapter.out.persistence;

import de.jcss.hbt.TestcontainersConfiguration;
import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.ExpenseCategory;
import de.jcss.hbt.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcBudgetItemRepositoryTest {

    @Autowired
    private JdbcBudgetItemRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM budget_item").update();
    }

    @Test
    void storesAndRestoresItemsWithAllAttributes() {
        BudgetItem income = BudgetItem.income(BudgetItemId.generate(), "Gehalt", money("2500.00"), LocalDate.of(2026, 10, 1));
        BudgetItem expense = BudgetItem.expense(BudgetItemId.generate(), "Miete", money("850.50"),
                ExpenseCategory.LEBENSUNTERHALT, LocalDate.of(2026, 10, 3));

        repository.add(income);
        repository.add(expense);

        assertThat(repository.findByMonth(YearMonth.of(2026, 10)))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expense, income);
    }

    @Test
    void findsOnlyItemsOfTheRequestedMonthIncludingItsBoundaries() {
        repository.add(income("September", LocalDate.of(2026, 9, 30)));
        repository.add(income("Erster", LocalDate.of(2026, 10, 1)));
        repository.add(income("Letzter", LocalDate.of(2026, 10, 31)));
        repository.add(income("November", LocalDate.of(2026, 11, 1)));

        assertThat(repository.findByMonth(YearMonth.of(2026, 10)))
                .extracting(BudgetItem::description)
                .containsExactly("Letzter", "Erster");
    }

    @Test
    void removeReportsWhetherTheItemExisted() {
        BudgetItem item = income("Gehalt", LocalDate.of(2026, 10, 1));
        repository.add(item);

        assertThat(repository.remove(item.id())).isTrue();
        assertThat(repository.remove(item.id())).isFalse();
        assertThat(repository.findByMonth(YearMonth.of(2026, 10))).isEmpty();
    }

    private static BudgetItem income(String description, LocalDate date) {
        return BudgetItem.income(BudgetItemId.generate(), description, money("100.00"), date);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
