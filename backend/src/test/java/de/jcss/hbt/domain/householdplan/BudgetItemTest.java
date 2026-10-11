package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemTest {

    private final BudgetItemId id = BudgetItemId.generate();
    private final Money amount = Money.of(new BigDecimal("100.00"));

    @Test
    void createsIncomeWithoutCategory() {
        BudgetItem item = BudgetItem.income(id, "Gehalt", amount);

        assertThat(item.type()).isEqualTo(ItemType.INCOME);
        assertThat(item.category()).isEmpty();
        assertThat(item.description()).isEqualTo("Gehalt");
        assertThat(item.amount()).isEqualTo(amount);
    }

    @Test
    void createsExpenseWithCategory() {
        BudgetItem item = BudgetItem.expense(id, "Miete", amount, ExpenseCategory.LEBENSUNTERHALT);

        assertThat(item.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(item.category()).contains(ExpenseCategory.LEBENSUNTERHALT);
    }

    @Test
    void expenseRequiresCategory() {
        assertThatThrownBy(() -> BudgetItem.expense(id, "Miete", amount, null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("category");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> BudgetItem.income(id, "  ", amount))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsTooLongDescription() {
        String tooLong = "x".repeat(BudgetItem.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> BudgetItem.income(id, tooLong, amount))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", Money.ZERO))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, "Gehalt", Money.of(new BigDecimal("-1.00"))))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        BudgetItem first = BudgetItem.income(id, "Gehalt", amount);
        BudgetItem second = BudgetItem.income(id, "Andere Beschreibung", Money.of(new BigDecimal("1.00")));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
