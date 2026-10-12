package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemTest {

    private static final BudgetItemDescription SALARY = BudgetItemDescription.of("Gehalt");
    private static final BudgetItemDescription RENT = BudgetItemDescription.of("Miete");

    private final BudgetItemId id = BudgetItemId.generate();
    private final Money amount = Money.of(new BigDecimal("100.00"));

    @Test
    void createsIncomeWithoutCategory() {
        BudgetItem item = BudgetItem.income(id, SALARY, amount);

        assertThat(item.type()).isEqualTo(ItemType.INCOME);
        assertThat(item.category()).isEmpty();
        assertThat(item.description()).isEqualTo(SALARY);
        assertThat(item.amount()).isEqualTo(amount);
    }

    @Test
    void createsExpenseWithCategory() {
        BudgetItem item = BudgetItem.expense(id, RENT, amount, ExpenseCategory.LEBENSUNTERHALT);

        assertThat(item.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(item.category()).contains(ExpenseCategory.LEBENSUNTERHALT);
    }

    @Test
    void expenseRequiresCategory() {
        assertThatThrownBy(() -> BudgetItem.expense(id, RENT, amount, null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("category");
    }

    @Test
    void requiresDescription() {
        assertThatThrownBy(() -> BudgetItem.income(id, null, amount))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, Money.ZERO))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, Money.of(new BigDecimal("-1.00"))))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        BudgetItem first = BudgetItem.income(id, SALARY, amount);
        BudgetItem second = BudgetItem.income(id, BudgetItemDescription.of("Andere Beschreibung"),
                Money.of(new BigDecimal("1.00")));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
