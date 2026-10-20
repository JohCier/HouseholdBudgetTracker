package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.householdplan.TargetDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HouseholdPlanOverviewTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final Year YEAR = Year.of(2026);

    @Test
    void withNoItemsEverythingIsZeroAndNoTargetIsMissed() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(), Map.of());

        assertThat(overview.planId()).isEqualTo(PLAN_ID);
        assertThat(overview.year()).isEqualTo(YEAR);
        assertThat(overview.items()).isEmpty();
        assertThat(overview.totalIncome().isZero()).isTrue();
        assertThat(overview.totalExpense().isZero()).isTrue();
        assertThat(overview.balance().isZero()).isTrue();
        assertThat(overview.expensesExceedIncome()).isFalse();
        assertThat(overview.categoryBreakdowns())
                .extracting(CategoryBreakdown::category)
                .containsExactly(ExpenseCategory.values());
        assertThat(overview.categoryBreakdowns()).allSatisfy(breakdown -> {
            assertThat(breakdown.total().isZero()).isTrue();
            assertThat(breakdown.actualPercentageOfIncome()).isEqualTo(Percentage.ZERO);
            assertThat(breakdown.targetMet()).isTrue();
        });
    }

    @Test
    void calculatesTotalsAndBalance() {
        HouseholdPlanOverview overview = overview("2000.00",
                expense(ExpenseCategory.LEBENSUNTERHALT, "600.00"),
                expense(ExpenseCategory.LEBENSUNTERHALT, "400.00"),
                expense(ExpenseCategory.FREIZEIT_SPASS, "100.00"));

        assertThat(overview.totalIncome().value()).isEqualByComparingTo("2000.00");
        assertThat(overview.totalExpense().value()).isEqualByComparingTo("1100.00");
        assertThat(overview.balance().value()).isEqualByComparingTo("900.00");
        assertThat(overview.expensesExceedIncome()).isFalse();
    }

    @Test
    void flagsWhenExpensesExceedIncome() {
        HouseholdPlanOverview overview = overview("500.00", expense(ExpenseCategory.LEBENSUNTERHALT, "800.00"));

        assertThat(overview.expensesExceedIncome()).isTrue();
        assertThat(overview.balance().value()).isEqualByComparingTo("-300.00");
    }

    @Test
    void ceilingsAreMetUpToTheTargetShare() {
        HouseholdPlanOverview overview = overview("1000.00",
                // exactly at the 50% ceiling
                expense(ExpenseCategory.LEBENSUNTERHALT, "500.00"),
                // above the 30% ceiling, no surplus left
                expense(ExpenseCategory.FREIZEIT_SPASS, "350.00"),
                expense(ExpenseCategory.SPAREN_INVESTIEREN, "150.00"));

        CategoryBreakdown lebensunterhalt = breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT);
        assertThat(lebensunterhalt.total().value()).isEqualByComparingTo("500.00");
        assertThat(lebensunterhalt.actualPercentageOfIncome()).isEqualTo(Percentage.of(50));
        assertThat(lebensunterhalt.targetPercentageOfIncome()).isEqualTo(Percentage.of(50));
        assertThat(lebensunterhalt.targetDirection()).isEqualTo(TargetDirection.AT_MOST);
        assertThat(lebensunterhalt.targetMet()).isTrue();

        CategoryBreakdown freizeit = breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS);
        assertThat(freizeit.actualPercentageOfIncome()).isEqualTo(Percentage.of(35));
        assertThat(freizeit.targetMet()).isFalse();
    }

    @Test
    void savingsAreAFloorThatMayBeExceeded() {
        // The rest of the income is spent on living costs, so no surplus adds to savings (AW-10)
        HouseholdPlanOverview belowFloor = overview("1000.00",
                expense(ExpenseCategory.SPAREN_INVESTIEREN, "100.00"),
                expense(ExpenseCategory.LEBENSUNTERHALT, "900.00"));
        HouseholdPlanOverview aboveFloor = overview("1000.00",
                expense(ExpenseCategory.SPAREN_INVESTIEREN, "300.00"),
                expense(ExpenseCategory.LEBENSUNTERHALT, "700.00"));

        CategoryBreakdown tooLittle = breakdownFor(belowFloor, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(tooLittle.targetDirection()).isEqualTo(TargetDirection.AT_LEAST);
        assertThat(tooLittle.actualPercentageOfIncome()).isEqualTo(Percentage.of(10));
        assertThat(tooLittle.targetMet()).isFalse();

        CategoryBreakdown plenty = breakdownFor(aboveFloor, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(plenty.actualPercentageOfIncome()).isEqualTo(Percentage.of(30));
        assertThat(plenty.targetMet()).isTrue();
    }

    @Test
    void spendingWithoutIncomeMissesCeilings() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR,
                List.of(expense(ExpenseCategory.FREIZEIT_SPASS, "50.00")), Map.of());

        assertThat(breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS).targetMet()).isFalse();
        assertThat(breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT).targetMet()).isTrue();
        assertThat(breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN).targetMet()).isTrue();
    }

    @Test
    void sumsUpIncomeAndExpensesPerCategoryAndKeepsTheItemOrder() {
        OverviewItem salary = income("1500.00");
        OverviewItem bonus = income("500.00");
        OverviewItem rent = expense(ExpenseCategory.LEBENSUNTERHALT, "900.00");
        OverviewItem etf = expense(ExpenseCategory.SPAREN_INVESTIEREN, "400.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(etf, salary, rent, bonus), Map.of());

        assertThat(overview.items()).containsExactly(etf, salary, rent, bonus);
        assertThat(overview.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("1300.00"));
        assertThat(breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT).total()).isEqualTo(money("900.00"));
        // Savings also contain the surplus of 700.00 (AW-10)
        assertThat(breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN).total()).isEqualTo(money("1100.00"));
    }

    /** MJ-6, MJ-8, MJ-9 */
    @Test
    void showsTheTotalsPerMonthAndPerYear() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(
                income(ItemAmount.monthly(money("3000.00"))),
                expense(ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("850.00")))), Map.of());

        assertThat(overview.totalIncome()).isEqualTo(money("36000.00"));
        assertThat(overview.monthlyTotalIncome()).isEqualTo(money("3000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("10200.00"));
        assertThat(overview.monthlyTotalExpense()).isEqualTo(money("850.00"));
        assertThat(overview.balance()).isEqualTo(money("25800.00"));
        assertThat(overview.monthlyBalance()).isEqualTo(money("2150.00"));
    }

    /** AW-10 */
    @Test
    void savingsContainTheSurplusAndShowItAsUnplanned() {
        HouseholdPlanOverview overview = overview("2000.00",
                expense(ExpenseCategory.LEBENSUNTERHALT, "1000.00"),
                expense(ExpenseCategory.FREIZEIT_SPASS, "600.00"),
                expense(ExpenseCategory.SPAREN_INVESTIEREN, "100.00"));

        CategoryBreakdown sparen = breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(sparen.total()).isEqualTo(money("400.00"));
        assertThat(sparen.unplanned()).isEqualTo(money("300.00"));
        assertThat(sparen.actualPercentageOfIncome()).isEqualTo(Percentage.of(20));
        assertThat(sparen.targetMet()).isTrue();
        CategoryBreakdown freizeit = breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS);
        assertThat(freizeit.total()).isEqualTo(money("600.00"));
        assertThat(freizeit.unplanned()).isEqualTo(Money.ZERO);
        assertThat(freizeit.actualPercentageOfIncome()).isEqualTo(Percentage.of(30));
        assertThat(breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT).unplanned()).isEqualTo(Money.ZERO);
        assertThat(overview.totalExpense()).isEqualTo(money("1700.00"));
    }

    /** MJ-14 */
    @Test
    void breakdownsShowTheirTotalsPerMonthRounded() {
        HouseholdPlanOverview overview = overview("2000.00",
                expense(ExpenseCategory.LEBENSUNTERHALT, "1000.00"),
                expense(ExpenseCategory.SPAREN_INVESTIEREN, "100.00"));

        CategoryBreakdown sparen = breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(sparen.monthlyTotal()).isEqualTo(money("83.33"));
        assertThat(sparen.monthlyUnplanned()).isEqualTo(money("75.00"));
        CategoryBreakdown leben = breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT);
        assertThat(leben.monthlyTotal()).isEqualTo(money("83.33"));
        assertThat(leben.monthlyUnplanned()).isEqualTo(Money.ZERO);
    }

    private static HouseholdPlanOverview overview(String income, OverviewItem... expenses) {
        List<OverviewItem> items = new ArrayList<>(List.of(expenses));
        items.add(income(income));
        return HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());
    }

    private static OverviewItem income(String amount) {
        return income(ItemAmount.yearly(money(amount)));
    }

    private static OverviewItem income(ItemAmount amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Einnahme"), amount,
                ItemType.INCOME, null, null, null);
    }

    private static OverviewItem expense(ExpenseCategory category, String amount) {
        return expense(category, ItemAmount.yearly(money(amount)));
    }

    private static OverviewItem expense(ExpenseCategory category, ItemAmount amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Ausgabe"), amount,
                ItemType.EXPENSE, category, null, null);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static CategoryBreakdown breakdownFor(HouseholdPlanOverview overview, ExpenseCategory category) {
        return overview.categoryBreakdowns().stream()
                .filter(breakdown -> breakdown.category() == category)
                .findFirst()
                .orElseThrow();
    }
}
