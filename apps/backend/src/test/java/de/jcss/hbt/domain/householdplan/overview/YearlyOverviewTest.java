package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.Percentage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// docs/specs/jahresuebersicht.md
class YearlyOverviewTest {

    @Test
    void listsThePlansNewestYearFirst() { // JU-1
        YearlyOverview overview = YearlyOverview.of(List.of(
                plan(2024, summary("0.00", "0.00")),
                plan(2026, summary("0.00", "0.00")),
                plan(2025, summary("0.00", "0.00"))));

        assertThat(overview.years()).extracting(YearlyOverviewEntry::year)
                .containsExactly(Year.of(2026), Year.of(2025), Year.of(2024));
    }

    @Test
    void showsIncomeExpenseAndBalanceWithTheirChangeFromThePreviousYear() { // JU-2, JU-5
        YearlyOverview overview = YearlyOverview.of(List.of(
                plan(2025, summary("30000.00", "25000.00")),
                plan(2026, summary("30900.00", "24250.00"))));

        YearlyOverviewEntry entry = overview.years().getFirst();
        assertThat(entry.totalIncome()).isEqualTo(money("30900.00"));
        assertThat(entry.totalExpense()).isEqualTo(money("24250.00"));
        assertThat(entry.balance()).isEqualTo(money("6650.00"));
        assertThat(entry.expensesExceedIncome()).isFalse();
        assertThat(entry.incomeChange()).isEqualTo(Percentage.of(3));
        assertThat(entry.expenseChange()).isEqualTo(Percentage.of(-3));
        assertThat(entry.balanceChange()).isEqualTo(Percentage.of(33));
    }

    @Test
    void theOldestPlanHasNoChange() { // JU-4
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2025, summary("1000.00", "500.00")))).years().getFirst();

        assertThat(entry.incomeChange()).isNull();
        assertThat(entry.expenseChange()).isNull();
        assertThat(entry.balanceChange()).isNull();
    }

    @Test
    void comparesOnlyWithTheImmediatelyPrecedingYear() { // JU-4
        YearlyOverview overview = YearlyOverview.of(List.of(
                plan(2024, summary("1000.00", "500.00")),
                plan(2026, summary("2000.00", "500.00"))));

        assertThat(overview.years()).allSatisfy(entry -> {
            assertThat(entry.incomeChange()).isNull();
            assertThat(entry.expenseChange()).isNull();
            assertThat(entry.balanceChange()).isNull();
        });
    }

    @Test
    void noChangeFromZeroInThePreviousYear() { // JU-6
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(
                plan(2025, summary("0.00", "500.00")),
                plan(2026, summary("1000.00", "500.00")))).years().getFirst();

        assertThat(entry.incomeChange()).isNull();
        assertThat(entry.expenseChange()).isEqualTo(Percentage.ZERO);
    }

    @Test
    void deficitIsMarkedAndAGrowingNegativeBalanceIsAPositiveChange() { // JU-2, JU-5
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(
                plan(2025, summary("1000.00", "2000.00")),
                plan(2026, summary("1000.00", "1500.00")))).years().getFirst();

        assertThat(entry.balance()).isEqualTo(money("-500.00"));
        assertThat(entry.expensesExceedIncome()).isTrue();
        assertThat(entry.balanceChange()).isEqualTo(Percentage.of(50));
    }

    @Test
    void emptyPlanHasZeroTotals() { // JU-3
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, BudgetSummary.EMPTY))).years().getFirst();

        assertThat(entry.totalIncome()).isEqualTo(Money.ZERO);
        assertThat(entry.totalExpense()).isEqualTo(Money.ZERO);
        assertThat(entry.balance()).isEqualTo(Money.ZERO);
    }

    // docs/specs/jahresuebersicht-monatsbetraege.md
    @Test
    void monthlyTotalsAreTheYearlyTotalsDividedByTwelve() { // JM-3, JM-5
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(
                plan(2025, summary("30000.00", "25000.00")),
                plan(2026, summary("30900.00", "24250.00")))).years().getFirst();

        assertThat(entry.monthlyTotalIncome()).isEqualTo(money("2575.00"));
        assertThat(entry.monthlyTotalExpense()).isEqualTo(money("2020.83"));
        assertThat(entry.monthlyBalance()).isEqualTo(money("554.17"));
        assertThat(entry.balanceChange()).isEqualTo(Percentage.of(33));
    }

    @Test
    void monthlyBalanceOfADeficitIsNegative() { // JM-3, JM-4
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, summary("1000.00", "1100.00")))).years().getFirst();

        assertThat(entry.monthlyBalance()).isEqualTo(money("-8.33"));
        assertThat(entry.expensesExceedIncome()).isTrue();
    }

    // docs/specs/einmalposten.md
    @Test
    void eachYearHasTheTotalsOfItsOneTimeItems() { // EP-13
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, summary("36000.00", "0.00"))),
                List.of(oneTime(2026, "800.00", "1200.00"))).years().getFirst();

        assertThat(entry.totalIncome()).isEqualTo(money("36000.00"));
        assertThat(entry.balance()).isEqualTo(money("36000.00"));
        assertThat(entry.oneTimeIncome()).isEqualTo(money("800.00"));
        assertThat(entry.oneTimeExpense()).isEqualTo(money("1200.00"));
        assertThat(entry.oneTimeBalance()).isEqualTo(money("-400.00"));
        assertThat(entry.oneTimeExpensesExceedIncome()).isTrue();
    }

    @Test
    void aYearWithoutOneTimeItemsHasZeroOneTimeTotals() { // EP-13
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, summary("1000.00", "0.00")))).years().getFirst();

        assertThat(entry.oneTimeIncome()).isEqualTo(Money.ZERO);
        assertThat(entry.oneTimeExpense()).isEqualTo(Money.ZERO);
        assertThat(entry.oneTimeBalance()).isEqualTo(Money.ZERO);
        assertThat(entry.oneTimeExpensesExceedIncome()).isFalse();
    }

    @Test
    void listsAYearWithOnlyOneTimeItemsWithoutPlanFigures() { // EP-12
        YearlyOverview overview = YearlyOverview.of(List.of(plan(2026, summary("1000.00", "0.00"))),
                List.of(oneTime(2027, "2000.00", "0.00")));

        assertThat(overview.years()).extracting(YearlyOverviewEntry::year).containsExactly(Year.of(2027), Year.of(2026));
        YearlyOverviewEntry entry = overview.years().getFirst();
        assertThat(entry.hasPlan()).isFalse();
        assertThat(entry.planId()).isNull();
        assertThat(entry.totalIncome()).isNull();
        assertThat(entry.monthlyTotalIncome()).isNull();
        assertThat(entry.monthlyBalance()).isNull();
        assertThat(entry.incomeChange()).isNull();
        assertThat(entry.expensesExceedIncome()).isFalse();
        assertThat(entry.oneTimeBalance()).isEqualTo(money("2000.00"));
    }

    @Test
    void aYearWithOnlyOneTimeItemsIsNoPreviousYear() { // JU-4, EP-15
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, summary("1000.00", "500.00"))),
                List.of(oneTime(2025, "300.00", "0.00"))).years().getFirst();

        assertThat(entry.incomeChange()).isNull();
        assertThat(entry.balanceChange()).isNull();
    }

    @Test
    void oneTimeTotalsAreNotSpreadOverTheMonths() { // EP-14
        YearlyOverviewEntry entry = YearlyOverview.of(List.of(plan(2026, summary("1200.00", "0.00"))),
                List.of(oneTime(2026, "1200.00", "0.00"))).years().getFirst();

        assertThat(entry.monthlyTotalIncome()).isEqualTo(money("100.00"));
        assertThat(entry.oneTimeIncome()).isEqualTo(money("1200.00"));
    }

    private static YearlyOverview.OneTimeSummary oneTime(int year, String income, String expense) {
        return new YearlyOverview.OneTimeSummary(Year.of(year), money(income), money(expense));
    }

    private static YearlyOverview.PlanSummary plan(int year, BudgetSummary summary) {
        return new YearlyOverview.PlanSummary(HouseholdPlanId.generate(), Year.of(year), summary);
    }

    private static BudgetSummary summary(String income, String expense) {
        return BudgetSummary.EMPTY.withIncome(money(income)).withExpense(ExpenseCategory.LEBENSUNTERHALT, money(expense));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
