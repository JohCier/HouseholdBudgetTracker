package de.jcss.hbt.domain.householdplan;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetSummaryTest {

    @Test
    void emptySummaryHasAllCategoriesAtZero() {
        BudgetSummary summary = BudgetSummary.EMPTY;

        assertThat(summary.totalIncome()).isEqualTo(Money.ZERO);
        assertThat(summary.totalExpense()).isEqualTo(Money.ZERO);
        assertThat(summary.balance()).isEqualTo(Money.ZERO);
        assertThat(summary.expensesExceedIncome()).isFalse();
        assertThat(summary.expensesByCategory()).containsOnlyKeys(ExpenseCategory.values());
        for (ExpenseCategory category : ExpenseCategory.values()) {
            assertThat(summary.expensesIn(category)).isEqualTo(Money.ZERO);
            assertThat(summary.shareOfIncome(category)).isEqualTo(Percentage.ZERO);
        }
    }

    @Test
    void sumsUpIncomeAndExpensesPerCategory() {
        BudgetSummary summary = BudgetSummary.EMPTY
                .withIncome(money("1500.00"))
                .withIncome(money("500.00"))
                .withExpense(ExpenseCategory.LEBENSUNTERHALT, money("600.00"))
                .withExpense(ExpenseCategory.LEBENSUNTERHALT, money("400.00"))
                .withExpense(ExpenseCategory.FREIZEIT_SPASS, money("100.00"));

        assertThat(summary.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(summary.expensesIn(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(money("1000.00"));
        assertThat(summary.expensesIn(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(money("100.00"));
        assertThat(summary.expensesIn(ExpenseCategory.SPAREN_INVESTIEREN)).isEqualTo(Money.ZERO);
        assertThat(summary.totalExpense()).isEqualTo(money("1100.00"));
        assertThat(summary.balance()).isEqualTo(money("900.00"));
        assertThat(summary.expensesExceedIncome()).isFalse();
    }

    @Test
    void balanceTurnsNegativeWhenExpensesExceedIncome() {
        BudgetSummary summary = BudgetSummary.EMPTY
                .withIncome(money("500.00"))
                .withExpense(ExpenseCategory.LEBENSUNTERHALT, money("800.00"));

        assertThat(summary.expensesExceedIncome()).isTrue();
        assertThat(summary.balance()).isEqualTo(money("-300.00"));
    }

    @Test
    void judgesEachCategoryAgainstItsTargetShare() {
        BudgetSummary summary = BudgetSummary.EMPTY
                .withIncome(money("1000.00"))
                .withExpense(ExpenseCategory.LEBENSUNTERHALT, money("500.00"))
                .withExpense(ExpenseCategory.FREIZEIT_SPASS, money("350.00"))
                .withExpense(ExpenseCategory.SPAREN_INVESTIEREN, money("100.00"));
        // The surplus of 50.00 counts towards leisure (AW-10)

        assertThat(summary.shareOfIncome(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(Percentage.of(50));
        assertThat(summary.isTargetMet(ExpenseCategory.LEBENSUNTERHALT)).isTrue();
        assertThat(summary.shareOfIncome(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(Percentage.of(40));
        assertThat(summary.isTargetMet(ExpenseCategory.FREIZEIT_SPASS)).isFalse();
        assertThat(summary.shareOfIncome(ExpenseCategory.SPAREN_INVESTIEREN)).isEqualTo(Percentage.of(10));
        assertThat(summary.isTargetMet(ExpenseCategory.SPAREN_INVESTIEREN)).isFalse();
    }

    /** AW-10 */
    @Test
    void surplusCountsTowardsLeisure() {
        BudgetSummary summary = BudgetSummary.EMPTY
                .withIncome(money("2000.00"))
                .withExpense(ExpenseCategory.LEBENSUNTERHALT, money("1200.00"));

        assertThat(summary.surplus()).isEqualTo(money("800.00"));
        assertThat(summary.unplannedIn(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(money("800.00"));
        assertThat(summary.assessedIn(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(money("800.00"));
        assertThat(summary.shareOfIncome(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(Percentage.of(40));
        assertThat(summary.isTargetMet(ExpenseCategory.FREIZEIT_SPASS)).isFalse();
        // Savings only count what was entered as such
        assertThat(summary.unplannedIn(ExpenseCategory.SPAREN_INVESTIEREN)).isEqualTo(Money.ZERO);
        assertThat(summary.shareOfIncome(ExpenseCategory.SPAREN_INVESTIEREN)).isEqualTo(Percentage.ZERO);
        assertThat(summary.isTargetMet(ExpenseCategory.SPAREN_INVESTIEREN)).isFalse();
        // Totals contain only what was entered
        assertThat(summary.expensesIn(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(Money.ZERO);
        assertThat(summary.totalExpense()).isEqualTo(money("1200.00"));
        assertThat(summary.balance()).isEqualTo(money("800.00"));
    }

    /** AW-10 */
    @Test
    void thereIsNoSurplusWithADeficitOrABalancedPlan() {
        BudgetSummary deficit = BudgetSummary.EMPTY
                .withIncome(money("500.00"))
                .withExpense(ExpenseCategory.FREIZEIT_SPASS, money("800.00"));
        BudgetSummary balanced = BudgetSummary.EMPTY
                .withIncome(money("500.00"))
                .withExpense(ExpenseCategory.FREIZEIT_SPASS, money("500.00"));

        assertThat(deficit.surplus()).isEqualTo(Money.ZERO);
        assertThat(deficit.assessedIn(ExpenseCategory.FREIZEIT_SPASS)).isEqualTo(money("800.00"));
        assertThat(deficit.assessedIn(ExpenseCategory.SPAREN_INVESTIEREN)).isEqualTo(Money.ZERO);
        assertThat(balanced.surplus()).isEqualTo(Money.ZERO);
    }

    @Test
    void isImmutable() {
        BudgetSummary empty = BudgetSummary.EMPTY;

        empty.withIncome(money("100.00"));

        assertThat(empty.totalIncome()).isEqualTo(Money.ZERO);
        assertThatThrownBy(() -> empty.expensesByCategory().put(ExpenseCategory.FREIZEIT_SPASS, Money.ZERO))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equalityIsBasedOnTheValues() {
        BudgetSummary explicit = new BudgetSummary(money("10.00"),
                Map.of(ExpenseCategory.FREIZEIT_SPASS, money("5.00"), ExpenseCategory.LEBENSUNTERHALT, Money.ZERO));
        BudgetSummary built = BudgetSummary.EMPTY
                .withIncome(money("10.00"))
                .withExpense(ExpenseCategory.FREIZEIT_SPASS, money("5.00"));

        assertThat(explicit).isEqualTo(built).hasSameHashCodeAs(built);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> new BudgetSummary(money("-1.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BudgetSummary.EMPTY.withExpense(ExpenseCategory.FREIZEIT_SPASS, money("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
