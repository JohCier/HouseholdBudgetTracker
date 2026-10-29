package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.CopiedHouseholdPlan;
import de.jcss.hbt.application.householdplan.CreatedHouseholdPlan;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.command.CopyHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.DeleteHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.PurgeHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RestoreHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlanEntry;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlans;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creating, copying and deleting plans, and the responses common to all requests (401, 404, 400).
 */
class HouseholdPlanControllerTest extends HouseholdPlanControllerTestSupport {

    @Test
    void requestsWithoutSessionAreRejectedWith401() throws Exception {
        anonymous.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Not signed in"));
        anonymous.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(plans);
    }

    /** PU-8, PU-15 */
    @Test
    void copyPlanReturns201WithIdAndOverviewOfTheNewPlan() throws Exception {
        HouseholdPlanId target = HouseholdPlanId.generate();
        when(plans.copyPlan(new CopyHouseholdPlanCommand(OWNER, PLAN_ID, Year.of(2027), false)))
                .thenReturn(new CopiedHouseholdPlan(target, false, EMPTY_OVERVIEW));

        mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(target.value().toString()))
                .andExpect(jsonPath("$.overview.year").value(2026));
    }

    /** PU-9 */
    @Test
    void copyPlanReturns200IfItOverwroteThePlanOfTheTargetYear() throws Exception {
        when(plans.copyPlan(new CopyHouseholdPlanCommand(OWNER, PLAN_ID, Year.of(2027), true)))
                .thenReturn(new CopiedHouseholdPlan(PLAN_ID, true, EMPTY_OVERVIEW));

        mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027,"overwrite":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));
    }

    /** PU-9 */
    @Test
    void copyPlanReturns409IfTheTargetYearHasAPlanThatMayNotBeOverwritten() throws Exception {
        when(plans.copyPlan(any())).thenThrow(new HouseholdPlanAlreadyExistsException(Year.of(2027)));

        mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027,"overwrite":false}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-already-exists"));
    }

    /** PU-2, HP-1 */
    @Test
    void copyPlanRejectsTargetYearsOutOfRangeOrMissing() throws Exception {
        for (String body : List.of("{\"targetYear\":1899}", "{\"targetYear\":2201}", "{}")) {
            mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(plans);
    }

    /** PU-2 */
    @Test
    void copyPlanIntoTheYearOfTheSourceReturns400() throws Exception {
        when(plans.copyPlan(any())).thenThrow(new DomainValidationException("the target year must differ"));

        mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2026}
                                """))
                .andExpect(status().isBadRequest());
    }

    /** HP-4 */
    @Test
    void copyPlanOfSomeoneElseReturns404() throws Exception {
        when(plans.copyPlan(any())).thenThrow(new HouseholdPlanNotFoundException(PLAN_ID));

        mockMvc.perform(post("/api/plans/{planId}/copies", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}
                                """))
                .andExpect(status().isNotFound());
    }

    /** AU-1, AU-3, AU-4 */
    @Test
    void createPlanReturns201WithIdAndOverview() throws Exception {
        when(plans.createPlan(new CreateHouseholdPlanCommand(OWNER, Year.of(2026))))
                .thenReturn(new CreatedHouseholdPlan(PLAN_ID, EMPTY_OVERVIEW));

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PLAN.toString()))
                .andExpect(jsonPath("$.overview.id").value(PLAN.toString()))
                .andExpect(jsonPath("$.overview.year").value(2026))
                .andExpect(jsonPath("$.overview.items").isEmpty());
    }

    /** FM-2 */
    @Test
    void createPlanForExistingYearReturns409() throws Exception {
        when(plans.createPlan(any())).thenThrow(new HouseholdPlanAlreadyExistsException(Year.of(2026)));

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-already-exists"))
                .andExpect(jsonPath("$.detail").value("A household plan for 2026 already exists"));
    }

    /** HP-1 */
    @Test
    void createPlanRejectsYearOutOfRange() throws Exception {
        for (String year : new String[] {"1899", "2201"}) {
            mockMvc.perform(post("/api/plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"year\":" + year + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                    .andExpect(jsonPath("$.errors.year").exists());
        }

        verifyNoInteractions(plans);
    }

    /** FM-2 */
    @Test
    void createPlanRequiresYear() throws Exception {
        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                .andExpect(jsonPath("$.errors.year").exists());

        verifyNoInteractions(plans);
    }

    @Test
    void domainValidationErrorReturns400WithDetail() throws Exception {
        when(plans.addIncome(any())).thenThrow(new DomainValidationException("amount must not have more than 2 decimal places"));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00","period":"YEARLY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                .andExpect(jsonPath("$.detail").value("amount must not have more than 2 decimal places"));
    }

    /** FM-2 */
    @Test
    void unknownPlanReturns404() throws Exception {
        when(plans.addIncome(any())).thenThrow(new HouseholdPlanNotFoundException(PLAN_ID));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00","period":"YEARLY"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-not-found"))
                .andExpect(jsonPath("$.detail").value("Household plan not found: " + PLAN));
    }

    /** PL-1, PL-2 */
    @Test
    void deletePlanReturns204() throws Exception {
        mockMvc.perform(delete("/api/plans/{planId}", PLAN))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(plans).deletePlan(new DeleteHouseholdPlanCommand(OWNER, PLAN_ID));
    }

    /** GP-14 */
    @Test
    void restorePlanReturns204AndPassesWhetherToOverwrite() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/restore", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"overwrite\": true}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/plans/{planId}/restore", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        verify(plans).restorePlan(new RestoreHouseholdPlanCommand(OWNER, PLAN_ID, true));
        verify(plans).restorePlan(new RestoreHouseholdPlanCommand(OWNER, PLAN_ID, false));
    }

    /** GP-14 */
    @Test
    void restorePlanOverAnActivePlanWithoutOverwriteReturns409() throws Exception {
        doThrow(new HouseholdPlanAlreadyExistsException(Year.of(2026))).when(plans).restorePlan(any());

        mockMvc.perform(post("/api/plans/{planId}/restore", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-already-exists"));
    }

    /** GP-17 */
    @Test
    void purgePlanReturns204() throws Exception {
        mockMvc.perform(delete("/api/plans/deleted/{planId}", PLAN))
                .andExpect(status().isNoContent());

        verify(plans).purgePlan(new PurgeHouseholdPlanCommand(OWNER, PLAN_ID));
    }

    /** GP-10, GP-18 */
    @Test
    void restoringOrPurgingAPlanThatIsNotADeletedOneReturns404() throws Exception {
        doThrow(new HouseholdPlanNotFoundException(PLAN_ID)).when(plans).restorePlan(any());
        doThrow(new HouseholdPlanNotFoundException(PLAN_ID)).when(plans).purgePlan(any());

        mockMvc.perform(post("/api/plans/{planId}/restore", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/plans/deleted/{planId}", PLAN))
                .andExpect(status().isNotFound());
    }

    /** GP-22 */
    @Test
    void deletedPlansListsThemWithTheirTotals() throws Exception {
        when(plans.getDeletedPlans(OWNER)).thenReturn(new DeletedPlans(List.of(DeletedPlanEntry.of(PLAN_ID,
                Year.of(2026), Instant.parse("2026-10-29T20:15:00Z"), 2, BudgetSummary.EMPTY
                        .withIncome(Money.of(new BigDecimal("38400.00")))
                        .withExpense(ExpenseCategory.LEBENSUNTERHALT, Money.of(new BigDecimal("34200.00")))))));

        mockMvc.perform(get("/api/plans/deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(1))
                .andExpect(jsonPath("$.plans[0].planId").value(PLAN.toString()))
                .andExpect(jsonPath("$.plans[0].year").value(2026))
                .andExpect(jsonPath("$.plans[0].deletedAt").value("2026-10-29T20:15:00Z"))
                .andExpect(jsonPath("$.plans[0].itemCount").value(2))
                .andExpect(jsonPath("$.plans[0].totalIncome").value("38400.00"))
                .andExpect(jsonPath("$.plans[0].balance").value("4200.00"))
                .andExpect(jsonPath("$.plans[0].monthlyTotalIncome").value("3200.00"))
                .andExpect(jsonPath("$.plans[0].monthlyBalance").value("350.00"))
                .andExpect(jsonPath("$.plans[0].expensesExceedIncome").value(false));
    }

    /** PL-1, HP-4 */
    @Test
    void deleteUnknownPlanReturns404() throws Exception {
        doThrow(new HouseholdPlanNotFoundException(PLAN_ID)).when(plans).deletePlan(any());

        mockMvc.perform(delete("/api/plans/{planId}", PLAN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-not-found"));
    }
}
