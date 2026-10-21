package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Overview of a plan and yearly overview of all plans.
 */
class HouseholdPlanOverviewControllerTest extends HouseholdPlanControllerTestSupport {

    /** UK-9 to UK-12 */
    @Test
    void yearlyOverviewListsThePlansWithTheirChanges() throws Exception { // JU-1, JU-2, JU-5, JU-6
        HouseholdPlanId previous = HouseholdPlanId.generate();
        when(plans.getYearlyOverview(OWNER)).thenReturn(YearlyOverview.of(List.of(
                new YearlyOverview.PlanSummary(previous, Year.of(2025), BudgetSummary.EMPTY
                        .withExpense(ExpenseCategory.LEBENSUNTERHALT, Money.of(new BigDecimal("25000.00")))),
                new YearlyOverview.PlanSummary(PLAN_ID, Year.of(2026), BudgetSummary.EMPTY
                        .withIncome(Money.of(new BigDecimal("30900.00")))
                        .withExpense(ExpenseCategory.LEBENSUNTERHALT, Money.of(new BigDecimal("24250.00")))))));

        mockMvc.perform(get("/api/plans/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years.length()").value(2))
                .andExpect(jsonPath("$.years[0].planId").value(PLAN.toString()))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].totalIncome").value("30900.00"))
                .andExpect(jsonPath("$.years[0].totalExpense").value("24250.00"))
                .andExpect(jsonPath("$.years[0].balance").value("6650.00"))
                .andExpect(jsonPath("$.years[0].monthlyTotalIncome").value("2575.00"))
                .andExpect(jsonPath("$.years[0].monthlyTotalExpense").value("2020.83"))
                .andExpect(jsonPath("$.years[0].monthlyBalance").value("554.17"))
                .andExpect(jsonPath("$.years[0].expensesExceedIncome").value(false))
                .andExpect(jsonPath("$.years[0].incomeChange").isEmpty())
                .andExpect(jsonPath("$.years[0].expenseChange").value(-3.0))
                .andExpect(jsonPath("$.years[0].balanceChange").value(126.6))
                .andExpect(jsonPath("$.years[1].year").value(2025))
                .andExpect(jsonPath("$.years[1].expensesExceedIncome").value(true));
    }

    @Test
    void yearlyOverviewNeedsASignedInUser() throws Exception {
        anonymous.perform(get("/api/plans/years")).andExpect(status().isUnauthorized());
        verifyNoInteractions(plans);
    }

    @Test
    void overviewListsTheGroupedEntriesWithSums() throws Exception {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Wartung"),
                        yearly("400.00"), ItemType.EXPENSE, ExpenseCategory.LEBENSUNTERHALT,
                        BudgetItemSubcategory.of("Auto"), null),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        yearly("3000.00"), ItemType.INCOME, null, null, null)), Map.of());
        when(plans.getOverview(OWNER, Year.of(2026))).thenReturn(overview);

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].subcategory").value("Auto"))
                .andExpect(jsonPath("$.items[1].subcategory").isEmpty())
                .andExpect(jsonPath("$.incomeGroups[0].subcategory").value("Sonstiges"))
                .andExpect(jsonPath("$.incomeGroups[0].total").value("3000.00"))
                .andExpect(jsonPath("$.incomeGroups[0].items[0].description").value("Gehalt"))
                .andExpect(jsonPath("$.expenseCategories.length()").value(1))
                .andExpect(jsonPath("$.expenseCategories[0].category").value("LEBENSUNTERHALT"))
                .andExpect(jsonPath("$.expenseCategories[0].total").value("400.00"))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].subcategory").value("Auto"))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].total").value("400.00"))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].percentageOfSection").value(100.0))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].items[0].percentageOfGroup").value(100.0))
                .andExpect(jsonPath("$.items[0].percentageOfGroup").isEmpty());
    }

    /** MJ-9, MJ-12, MJ-13 */
    @Test
    void overviewShowsMonthlyAndYearlyAmountsAndTotals() throws Exception {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Versicherung"),
                        yearly("1000.00"), ItemType.EXPENSE, ExpenseCategory.LEBENSUNTERHALT, null, null),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        ItemAmount.monthly(Money.of(new BigDecimal("3000.00"))), ItemType.INCOME, null, null, null)),
                Map.of());
        when(plans.getOverview(OWNER, Year.of(2026))).thenReturn(overview);

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].amount").value("1000.00"))
                .andExpect(jsonPath("$.items[0].period").value("YEARLY"))
                .andExpect(jsonPath("$.items[0].monthlyAmount").value("83.33"))
                .andExpect(jsonPath("$.items[0].annualAmount").value("1000.00"))
                .andExpect(jsonPath("$.items[1].amount").value("3000.00"))
                .andExpect(jsonPath("$.items[1].period").value("MONTHLY"))
                .andExpect(jsonPath("$.items[1].monthlyAmount").value("3000.00"))
                .andExpect(jsonPath("$.items[1].annualAmount").value("36000.00"))
                .andExpect(jsonPath("$.totalIncome").value("36000.00"))
                .andExpect(jsonPath("$.monthlyTotalIncome").value("3000.00"))
                .andExpect(jsonPath("$.monthlyTotalExpense").value("83.33"))
                .andExpect(jsonPath("$.balance").value("35000.00"))
                .andExpect(jsonPath("$.monthlyBalance").value("2916.67"))
                .andExpect(jsonPath("$.incomeGroups[0].monthlyTotal").value("3000.00"))
                .andExpect(jsonPath("$.expenseCategories[0].monthlyTotal").value("83.33"))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].monthlyTotal").value("83.33"));
    }

    @Test
    void overviewOfAYearReturnsTotalsBreakdownsAndItems() throws Exception {
        BudgetItemId itemId = BudgetItemId.generate();
        when(plans.getOverview(OWNER, Year.of(2026))).thenReturn(HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(itemId, BudgetItemDescription.of("ETF"), yearly("500"),
                        ItemType.EXPENSE, ExpenseCategory.SPAREN_INVESTIEREN, null, new BudgetItemNote("ETF-Sparplan")),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        yearly("1000"), ItemType.INCOME, null, null, null)), Map.of()));

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalIncome").value("1000.00"))
                .andExpect(jsonPath("$.totalExpense").value("500.00"))
                .andExpect(jsonPath("$.balance").value("500.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false))
                .andExpect(jsonPath("$.categoryBreakdowns[2].category").value("SPAREN_INVESTIEREN"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].targetDirection").value("AT_LEAST"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].targetMet").value(true))
                .andExpect(jsonPath("$.categoryBreakdowns[2].total").value("1000.00"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].monthlyTotal").value("83.33"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].unplanned").value("500.00"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].monthlyUnplanned").value("41.67"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].unplannedPercentageOfTotal").value(50.01))
                .andExpect(jsonPath("$.items[0].id").value(itemId.value().toString()))
                .andExpect(jsonPath("$.items[0].amount").value("500.00"))
                .andExpect(jsonPath("$.items[0].category").value("SPAREN_INVESTIEREN"))
                .andExpect(jsonPath("$.items[0].note").value("ETF-Sparplan"))
                .andExpect(jsonPath("$.items[1].note").isEmpty())
                .andExpect(jsonPath("$.items[1].type").value("INCOME"))
                .andExpect(jsonPath("$.items[1].category").isEmpty());
    }

    @Test
    void overviewOfAYearWithoutPlanReturns404() throws Exception {
        when(plans.getOverview(OWNER, Year.of(2030))).thenThrow(new HouseholdPlanNotFoundException(Year.of(2030)));

        mockMvc.perform(get("/api/plans").param("year", "2030"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No household plan for 2030"));
    }

    /** HP-1, FM-2 */
    @Test
    void overviewRejectsYearOutOfRange() throws Exception {
        mockMvc.perform(get("/api/plans").param("year", "1899"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"));
        mockMvc.perform(get("/api/plans").param("year", "2201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"));

        verifyNoInteractions(plans);
    }

    @Test
    void overviewRequiresYear() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(plans);
    }
}
