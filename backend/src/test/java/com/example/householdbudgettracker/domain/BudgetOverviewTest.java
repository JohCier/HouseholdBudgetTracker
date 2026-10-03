package com.example.householdbudgettracker.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetOverviewTest {

    @Test
    void withNoItemsEverythingIsZero() {
        BudgetOverview overview = BudgetOverview.of(List.of());

        assertThat(overview.totalIncome().isZero()).isTrue();
        assertThat(overview.totalExpense().isZero()).isTrue();
        assertThat(overview.balance().isZero()).isTrue();
        assertThat(overview.expensesExceedIncome()).isFalse();
        assertThat(overview.categoryBreakdowns()).allSatisfy(breakdown -> {
            assertThat(breakdown.total().isZero()).isTrue();
            assertThat(breakdown.actualPercentageOfIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(breakdown.withinTarget()).isTrue();
        });
    }

    @Test
    void calculatesTotalsAndBalance() {
        List<BudgetItem> items = List.of(
                BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("2000.00"))),
                BudgetItem.expense(BudgetItemId.generate(), "Miete", Money.of(new BigDecimal("1000.00")), ExpenseCategory.LEBENSUNTERHALT),
                BudgetItem.expense(BudgetItemId.generate(), "Kino", Money.of(new BigDecimal("100.00")), ExpenseCategory.FREIZEIT_SPASS)
        );

        BudgetOverview overview = BudgetOverview.of(items);

        assertThat(overview.totalIncome().value()).isEqualByComparingTo("2000.00");
        assertThat(overview.totalExpense().value()).isEqualByComparingTo("1100.00");
        assertThat(overview.balance().value()).isEqualByComparingTo("900.00");
        assertThat(overview.expensesExceedIncome()).isFalse();
    }

    @Test
    void flagsWhenExpensesExceedIncome() {
        List<BudgetItem> items = List.of(
                BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("500.00"))),
                BudgetItem.expense(BudgetItemId.generate(), "Miete", Money.of(new BigDecimal("800.00")), ExpenseCategory.LEBENSUNTERHALT)
        );

        BudgetOverview overview = BudgetOverview.of(items);

        assertThat(overview.expensesExceedIncome()).isTrue();
        assertThat(overview.balance().value()).isEqualByComparingTo("-300.00");
    }

    @Test
    void breaksDownExpensesPerCategoryAgainstFiftyThirtyTwentyRule() {
        List<BudgetItem> items = List.of(
                BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("1000.00"))),
                // exactly at the 50% target for LEBENSUNTERHALT
                BudgetItem.expense(BudgetItemId.generate(), "Miete", Money.of(new BigDecimal("500.00")), ExpenseCategory.LEBENSUNTERHALT),
                // exceeds the 30% target for FREIZEIT_SPASS (350 > 300)
                BudgetItem.expense(BudgetItemId.generate(), "Konzerte", Money.of(new BigDecimal("350.00")), ExpenseCategory.FREIZEIT_SPASS),
                // below the 20% target for SPAREN_INVESTIEREN
                BudgetItem.expense(BudgetItemId.generate(), "ETF-Sparplan", Money.of(new BigDecimal("100.00")), ExpenseCategory.SPAREN_INVESTIEREN)
        );

        BudgetOverview overview = BudgetOverview.of(items);

        CategoryBreakdown lebensunterhalt = breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT);
        assertThat(lebensunterhalt.total().value()).isEqualByComparingTo("500.00");
        assertThat(lebensunterhalt.actualPercentageOfIncome()).isEqualByComparingTo("50.00");
        assertThat(lebensunterhalt.targetPercentageOfIncome()).isEqualTo(50);
        assertThat(lebensunterhalt.withinTarget()).isTrue();

        CategoryBreakdown freizeit = breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS);
        assertThat(freizeit.actualPercentageOfIncome()).isEqualByComparingTo("35.00");
        assertThat(freizeit.withinTarget()).isFalse();

        CategoryBreakdown sparen = breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(sparen.actualPercentageOfIncome()).isEqualByComparingTo("10.00");
        assertThat(sparen.withinTarget()).isTrue();
    }

    private CategoryBreakdown breakdownFor(BudgetOverview overview, ExpenseCategory category) {
        return overview.categoryBreakdowns().stream()
                .filter(breakdown -> breakdown.category() == category)
                .findFirst()
                .orElseThrow();
    }
}
