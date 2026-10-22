package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemMarks;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.SavingPotential;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeSavingPotentialCommand;
import de.jcss.hbt.domain.householdplan.command.MarkBudgetItemImportantCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Marking budget items as important and recording their saving potential.
 */
class BudgetItemMarksControllerTest extends HouseholdPlanControllerTestSupport {

    /** PM-1, PM-4a */
    @Test
    void markImportantReturns200WithOverview() throws Exception {
        UUID item = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/important", PLAN, item)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"important":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));

        verify(plans).markItemImportant(new MarkBudgetItemImportantCommand(OWNER, PLAN_ID, BudgetItemId.of(item), true));
    }

    @Test
    void markImportantRequiresTheMark() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/important", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(plans);
    }

    /** PM-2a, PM-4b */
    @Test
    void changeSavingPotentialPassesTheMonthlyAmount() throws Exception {
        UUID item = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", PLAN, item)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlyAmount":"5.00"}
                                """))
                .andExpect(status().isOk());

        verify(plans).changeSavingPotential(new ChangeSavingPotentialCommand(OWNER, PLAN_ID, BudgetItemId.of(item),
                SavingPotential.monthly(Money.of(new BigDecimal("5.00")))));
    }

    /** PM-4b */
    @Test
    void removeSavingPotentialPassesNone() throws Exception {
        UUID item = UUID.randomUUID();

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}/saving-potential", PLAN, item))
                .andExpect(status().isOk());

        verify(plans).changeSavingPotential(new ChangeSavingPotentialCommand(OWNER, PLAN_ID, BudgetItemId.of(item), null));
    }

    /** PM-2a */
    @Test
    void invalidSavingPotentialReturns400() throws Exception {
        for (String body : List.of("{}", "{\"monthlyAmount\":0}", "{\"monthlyAmount\":-1}", "{\"monthlyAmount\":\"1.001\"}")) {
            mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", PLAN, UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(plans);
    }

    /** PM-2: an income, or more than the monthly amount */
    @Test
    void savingPotentialNotFittingTheItemReturns400() throws Exception {
        when(plans.changeSavingPotential(any()))
                .thenThrow(new DomainValidationException("income items must not have a saving potential"));

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monthlyAmount":"5.00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"));
    }

    @Test
    void markingAnUnknownItemReturns404() throws Exception {
        when(plans.markItemImportant(any())).thenThrow(new BudgetItemNotFoundException(BudgetItemId.generate()));

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/important", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"important":false}
                                """))
                .andExpect(status().isNotFound());
    }

    /** PM-6: a restored item keeps its marks, a new one has none */
    @Test
    void addItemPassesTheMarksOfARestoredItem() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Streaming","amount":"15","period":"MONTHLY","category":"FREIZEIT_SPASS",
                                 "important":true,"savingPotential":"5.00"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000","period":"MONTHLY","important":true}
                                """))
                .andExpect(status().isCreated());

        ItemAmount streaming = ItemAmount.monthly(Money.of(new BigDecimal("15")));
        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Streaming"),
                streaming, ExpenseCategory.FREIZEIT_SPASS, null, null,
                new ItemMarks(true, SavingPotential.monthly(Money.of(new BigDecimal("5.00"))))));
        verify(plans).addIncome(new AddIncomeCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Gehalt"),
                ItemAmount.monthly(Money.of(new BigDecimal("2000"))), null, null, new ItemMarks(true, null)));
    }

    /** PM-9, PM-10, PM-13, PM-14, SU-1, SU-4, SU-10 */
    @Test
    void overviewShowsTheMarksAndTheSavingPotential() throws Exception {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Streaming"),
                        ItemAmount.monthly(Money.of(new BigDecimal("15.00"))), ItemType.EXPENSE,
                        ExpenseCategory.FREIZEIT_SPASS, null, null,
                        new ItemMarks(true, SavingPotential.monthly(Money.of(new BigDecimal("5.00"))))),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Fitnessstudio"),
                        yearly("360.00"), ItemType.EXPENSE, ExpenseCategory.FREIZEIT_SPASS, null, null,
                        new ItemMarks(false, SavingPotential.monthly(Money.of(new BigDecimal("30.00"))))),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        yearly("1000.00"), ItemType.INCOME, null, null, null)),
                Map.of());
        when(plans.getOverview(OWNER, Year.of(2026))).thenReturn(overview);

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].important").value(true))
                .andExpect(jsonPath("$.items[0].savingPotential").value("5.00"))
                .andExpect(jsonPath("$.items[1].important").value(false))
                .andExpect(jsonPath("$.items[2].savingPotential").isEmpty())
                .andExpect(jsonPath("$.monthlySavingPotential").value("35.00"))
                .andExpect(jsonPath("$.savingPotential").value("420.00"))
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].monthlySavingPotential").value("35.00"))
                .andExpect(jsonPath("$.incomeGroups[0].monthlySavingPotential").value("0.00"))
                .andExpect(jsonPath("$.savingPotentialsBySubcategory.length()").value(1))
                .andExpect(jsonPath("$.savingPotentialsBySubcategory[0].subcategory").value("Sonstiges"))
                .andExpect(jsonPath("$.savingPotentialsBySubcategory[0].monthlyAmount").value("35.00"))
                .andExpect(jsonPath("$.savingPotentialsBySubcategory[0].amount").value("420.00"));
    }
}
