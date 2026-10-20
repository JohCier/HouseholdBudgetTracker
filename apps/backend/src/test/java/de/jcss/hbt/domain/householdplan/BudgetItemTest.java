package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemTest {

    private static final BudgetItemDescription SALARY = BudgetItemDescription.of("Gehalt");
    private static final BudgetItemDescription RENT = BudgetItemDescription.of("Miete");

    private final BudgetItemId id = BudgetItemId.generate();
    private final ItemAmount amount = yearly("100.00");

    @Test
    void createsIncomeWithoutCategory() {
        BudgetItem item = BudgetItem.income(id, SALARY, amount, null, null);

        assertThat(item.type()).isEqualTo(ItemType.INCOME);
        assertThat(item.category()).isEmpty();
        assertThat(item.description()).isEqualTo(SALARY);
        assertThat(item.amount()).isEqualTo(amount);
    }

    @Test
    void createsExpenseWithCategory() {
        BudgetItem item = BudgetItem.expense(id, RENT, amount, ExpenseCategory.LEBENSUNTERHALT, null, null);

        assertThat(item.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(item.category()).contains(ExpenseCategory.LEBENSUNTERHALT);
    }

    @Test
    void expenseRequiresCategory() {
        assertThatThrownBy(() -> BudgetItem.expense(id, RENT, amount, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("category");
    }

    @Test
    void requiresDescription() {
        assertThatThrownBy(() -> BudgetItem.income(id, null, amount, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, yearly("0"), null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, yearly("-1.00"), null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void reconstitutesIncomeAndExpense() {
        BudgetItem income = BudgetItem.reconstitute(id, SALARY, amount, ItemType.INCOME, null, null, null);
        BudgetItem expense = BudgetItem.reconstitute(id, RENT, amount, ItemType.EXPENSE, ExpenseCategory.FREIZEIT_SPASS, null, null);

        assertThat(income).usingRecursiveComparison().isEqualTo(BudgetItem.income(id, SALARY, amount, null, null));
        assertThat(expense).usingRecursiveComparison()
                .isEqualTo(BudgetItem.expense(id, RENT, amount, ExpenseCategory.FREIZEIT_SPASS, null, null));
    }

    @Test
    void reconstituteEnforcesTheSameRules() {
        assertThatThrownBy(() -> BudgetItem.reconstitute(id, SALARY, amount, ItemType.INCOME,
                ExpenseCategory.LEBENSUNTERHALT, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("category");
        assertThatThrownBy(() -> BudgetItem.reconstitute(id, RENT, amount, ItemType.EXPENSE, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("category");
        assertThatThrownBy(() -> BudgetItem.reconstitute(id, SALARY, yearly("0"), ItemType.INCOME, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("amount");
    }

    /** PB-1, PB-2, PB-3 */
    @Test
    void changedCopyKeepsIdAndTypeAndEnforcesTheSameRules() {
        BudgetItem expense = BudgetItem.expense(id, RENT, amount, ExpenseCategory.LEBENSUNTERHALT, null, null);
        Money newAmount = Money.of(new BigDecimal("900.00"));

        BudgetItem changed = expense.changedTo(SALARY, ItemAmount.yearly(newAmount), ExpenseCategory.FREIZEIT_SPASS,
                null, null);

        assertThat(changed.id()).isEqualTo(id);
        assertThat(changed.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(changed.description()).isEqualTo(SALARY);
        assertThat(changed.amount()).isEqualTo(ItemAmount.yearly(newAmount));
        assertThat(changed.category()).contains(ExpenseCategory.FREIZEIT_SPASS);
        assertThatThrownBy(() -> expense.changedTo(RENT, amount, null, null, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> BudgetItem.income(id, SALARY, amount, null, null)
                .changedTo(SALARY, amount, ExpenseCategory.LEBENSUNTERHALT, null, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void comparesValuesIgnoringTheId() {
        BudgetItem item = BudgetItem.expense(id, RENT, amount, ExpenseCategory.LEBENSUNTERHALT, null, null);

        assertThat(item.hasSameValuesAs(BudgetItem.expense(BudgetItemId.generate(), RENT,
                yearly("100"), ExpenseCategory.LEBENSUNTERHALT, null, null))).isTrue();
        assertThat(item.hasSameValuesAs(item.changedTo(RENT, amount, ExpenseCategory.FREIZEIT_SPASS, null, null))).isFalse();
        assertThat(item.hasSameValuesAs(item.changedTo(SALARY, amount, ExpenseCategory.LEBENSUNTERHALT, null, null))).isFalse();
        assertThat(item.hasSameValuesAs(BudgetItem.income(id, RENT, amount, null, null))).isFalse();
    }

    /** MJ-8 */
    @Test
    void addsTheYearlyAmountOfAMonthlyItemToTheSummary() {
        ItemAmount monthly = ItemAmount.monthly(Money.of(new BigDecimal("850.00")));

        BudgetSummary summary = BudgetItem.expense(id, RENT, monthly, ExpenseCategory.LEBENSUNTERHALT, null, null)
                .addTo(BudgetSummary.EMPTY);

        assertThat(summary.expensesIn(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(Money.of(new BigDecimal("10200.00")));
    }

    /** MJ-4 */
    @Test
    void aDifferentPeriodIsADifferentValue() {
        BudgetItem item = BudgetItem.income(id, SALARY, amount, null, null);

        assertThat(item.hasSameValuesAs(item.changedTo(SALARY, ItemAmount.monthly(amount.value()), null, null, null)))
                .isFalse();
    }

    @Test
    void addsItsAmountToTheSummary() {
        BudgetSummary summary = BudgetItem.expense(id, RENT, amount, ExpenseCategory.LEBENSUNTERHALT, null, null)
                .addTo(BudgetItem.income(BudgetItemId.generate(), SALARY, amount, null, null).addTo(BudgetSummary.EMPTY));

        assertThat(summary.totalIncome()).isEqualTo(amount.value());
        assertThat(summary.expensesIn(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(amount.value());
        assertThat(summary.totalExpense()).isEqualTo(amount.value());
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        BudgetItem first = BudgetItem.income(id, SALARY, amount, null, null);
        BudgetItem second = BudgetItem.income(id, BudgetItemDescription.of("Andere Beschreibung"),
                yearly("1.00"), null, null);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    private static ItemAmount yearly(String value) {
        return ItemAmount.yearly(Money.of(new BigDecimal(value)));
    }
}
