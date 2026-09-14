package com.example.householdbudgettracker.domain;

import com.example.householdbudgettracker.domain.exception.BudgetItemNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTest {

    @Test
    void startsEmpty() {
        Budget budget = Budget.createEmpty();

        assertThat(budget.items()).isEmpty();
    }

    @Test
    void addsIncomeAndExpenseItems() {
        Budget budget = Budget.createEmpty();

        BudgetItemId incomeId = budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));
        BudgetItemId expenseId = budget.addExpense("Miete", Money.of(new BigDecimal("800.00")), ExpenseCategory.LEBENSUNTERHALT);

        assertThat(budget.items()).hasSize(2);
        assertThat(budget.items()).extracting(BudgetItem::id).containsExactlyInAnyOrder(incomeId, expenseId);
    }

    @Test
    void removesExistingItem() {
        Budget budget = Budget.createEmpty();
        BudgetItemId incomeId = budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        budget.removeItem(incomeId);

        assertThat(budget.items()).isEmpty();
    }

    @Test
    void removingUnknownItemThrows() {
        Budget budget = Budget.createEmpty();

        assertThatThrownBy(() -> budget.removeItem(BudgetItemId.generate()))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    @Test
    void itemsListIsUnmodifiable() {
        Budget budget = Budget.createEmpty();
        budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        assertThatThrownBy(() -> budget.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void overviewReflectsCurrentItems() {
        Budget budget = Budget.createEmpty();
        budget.addIncome("Gehalt", Money.of(new BigDecimal("1000.00")));
        budget.addExpense("Miete", Money.of(new BigDecimal("500.00")), ExpenseCategory.LEBENSUNTERHALT);

        BudgetOverview overview = budget.overview();

        assertThat(overview.totalIncome().value()).isEqualByComparingTo("1000.00");
        assertThat(overview.totalExpense().value()).isEqualByComparingTo("500.00");
    }
}
