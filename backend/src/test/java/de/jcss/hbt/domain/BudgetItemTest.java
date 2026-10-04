package de.jcss.hbt.domain;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    private final BudgetItemId id = BudgetItemId.generate();
    private final Money amount = Money.of(new BigDecimal("100.00"));

    @Test
    void createsIncomeWithoutCategory() {
        BudgetItem item = BudgetItem.income(id, "Gehalt", amount, DATE);

        assertThat(item.type()).isEqualTo(ItemType.INCOME);
        assertThat(item.category()).isEmpty();
        assertThat(item.description()).isEqualTo("Gehalt");
        assertThat(item.amount()).isEqualTo(amount);
        assertThat(item.bookingDate()).isEqualTo(DATE);
    }

    @Test
    void createsExpenseWithCategory() {
        BudgetItem item = BudgetItem.expense(id, "Miete", amount, ExpenseCategory.LEBENSUNTERHALT, DATE);

        assertThat(item.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(item.category()).contains(ExpenseCategory.LEBENSUNTERHALT);
    }

    @Test
    void expenseRequiresCategory() {
        assertThatThrownBy(() -> BudgetItem.expense(id, "Miete", amount, null, DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("category");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> BudgetItem.income(id, "  ", amount, DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsTooLongDescription() {
        String tooLong = "x".repeat(BudgetItem.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> BudgetItem.income(id, tooLong, amount, DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", null, DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", Money.ZERO, DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", Money.of(new BigDecimal("-1.00")), DATE))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void requiresBookingDate() {
        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", amount, null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("booking date");
    }

    @Test
    void knowsItsBookingMonth() {
        BudgetItem item = BudgetItem.income(id, "Gehalt", amount, DATE);

        assertThat(item.isBookedIn(YearMonth.of(2026, 10))).isTrue();
        assertThat(item.isBookedIn(YearMonth.of(2026, 11))).isFalse();
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        BudgetItem first = BudgetItem.income(id, "Gehalt", amount, DATE);
        BudgetItem second = BudgetItem.income(id, "Andere Beschreibung", Money.of(new BigDecimal("1.00")), DATE.plusDays(1));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
