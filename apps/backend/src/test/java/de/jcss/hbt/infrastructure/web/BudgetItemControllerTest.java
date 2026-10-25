package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.AddedBudgetItem;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Adding, changing and removing budget items.
 */
class BudgetItemControllerTest extends HouseholdPlanControllerTestSupport {

    /** AU-1, AU-3, AU-4 */
    @Test
    void addIncomeReturns201WithIdAndOverview() throws Exception {
        BudgetItemId id = BudgetItemId.generate();
        when(plans.addIncome(any())).thenReturn(new AddedBudgetItem(id, HouseholdPlanOverview.of(PLAN_ID, Year.of(2026),
                List.of(new OverviewItem(id, BudgetItemDescription.of("Gehalt"), yearly("2000"),
                        ItemType.INCOME, null, null, null)), Map.of())));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000.00","period":"YEARLY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.overview.totalIncome").value("2000.00"))
                .andExpect(jsonPath("$.overview.items[0].id").value(id.value().toString()));
    }

    @Test
    void addExpenseMapsPlanAndCategory() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800,"period":"YEARLY","category":"LEBENSUNTERHALT"}
                                """))
                .andExpect(status().isCreated());

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Miete"),
                yearly("800"), ExpenseCategory.LEBENSUNTERHALT, null, null));
    }

    /** PN-1, PN-3 */
    @Test
    void addItemPassesTheNoteAndTreatsABlankOneAsNone() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800,"period":"YEARLY","category":"LEBENSUNTERHALT","note":"Warmmiete"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","note":"  "}
                                """))
                .andExpect(status().isCreated());

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Miete"),
                yearly("800"), ExpenseCategory.LEBENSUNTERHALT, null, new BudgetItemNote("Warmmiete")));
        verify(plans).addIncome(new AddIncomeCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Gehalt"),
                yearly("2000"), null, null));
    }

    /** UK-1, UK-2, UK-4 */
    @Test
    void itemRequestsPassTheSubcategoryAndTreatABlankOneAsNone() throws Exception {
        UUID itemId = UUID.randomUUID();
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Wartung","amount":400,"period":"YEARLY","category":"LEBENSUNTERHALT","subcategory":" Auto "}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","subcategory":""}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","subcategory":"Gehalt"}
                                """))
                .andExpect(status().isOk());

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Wartung"),
                yearly("400"), ExpenseCategory.LEBENSUNTERHALT, BudgetItemSubcategory.of("Auto"), null));
        verify(plans).addIncome(new AddIncomeCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Gehalt"),
                yearly("2000"), null, null));
        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), yearly("2000"), null,
                BudgetItemSubcategory.of("Gehalt"), null));
    }

    /** UK-2 */
    @Test
    void tooLongSubcategoryReturns400() throws Exception {
        String subcategory = "x".repeat(BudgetItemSubcategory.MAX_LENGTH + 1);

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","subcategory":"%s"}
                                """.formatted(subcategory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subcategory").exists());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","subcategory":"%s"}
                                """.formatted(subcategory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subcategory").exists());

        verifyNoInteractions(plans);
    }

    /** PN-2 */
    @Test
    void tooLongNoteReturns400() throws Exception {
        String note = "x".repeat(BudgetItemNote.MAX_LENGTH + 1);

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","note":"%s"}
                                """.formatted(note)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.note").exists());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"period":"YEARLY","note":"%s"}
                                """.formatted(note)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.note").exists());

        verifyNoInteractions(plans);
    }

    /** PN-4 */
    @Test
    void changeItemPassesTheNote() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2100","period":"YEARLY","note":"netto"}
                                """))
                .andExpect(status().isOk());

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), yearly("2100"), null, null, new BudgetItemNote("netto")));
    }

    @Test
    void invalidRequestReturnsProblemDetailListingAllFields() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.detail").value(containsString("amount")));

        verifyNoInteractions(plans);
    }

    /** MJ-1, MJ-2 */
    @Test
    void itemRequestsPassTheEnteredAmountWithItsPeriod() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"850","period":"MONTHLY","category":"LEBENSUNTERHALT"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"3000","period":"MONTHLY"}
                                """))
                .andExpect(status().isOk());

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Miete"),
                ItemAmount.monthly(Money.of(new BigDecimal("850"))), ExpenseCategory.LEBENSUNTERHALT, null, null));
        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), ItemAmount.monthly(Money.of(new BigDecimal("3000"))), null, null,
                null));
    }

    /** MJ-1 */
    @Test
    void missingOrUnknownPeriodReturns400() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.period").exists());
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000","period":"WEEKLY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(plans);
    }

    @Test
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"800","period":"YEARLY","category":"GIBTS_NICHT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /** AU-1, AU-4 */
    @Test
    void removeItemReturns200WithOverview() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", PLAN, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()))
                .andExpect(jsonPath("$.items").isEmpty());

        verify(plans).removeItem(new RemoveBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId)));
    }

    /** FM-2 */
    @Test
    void removeUnknownItemReturns404() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new BudgetItemNotFoundException(BudgetItemId.of(itemId))).when(plans).removeItem(any());

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", PLAN, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:item-not-found"))
                .andExpect(jsonPath("$.detail").value("Budget item not found: " + itemId));
    }

    /** PB-1, AU-1, AU-4 */
    @Test
    void changeItemReturns200WithOverviewAndMapsAllValues() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"900.00","period":"YEARLY","category":"SPAREN_INVESTIEREN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Miete"), yearly("900.00"), ExpenseCategory.SPAREN_INVESTIEREN, null, null));
    }

    /** PB-1 */
    @Test
    void changeItemWithoutCategoryPassesNone() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2100","period":"YEARLY"}
                                """))
                .andExpect(status().isOk());

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), yearly("2100"), null, null, null));
    }

    /** PB-3 */
    @Test
    void invalidChangeRequestReturns400() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":" ","amount":"10.005","period":"YEARLY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(plans);
    }

    /** PB-2, PB-3 */
    @Test
    void categoryNotFittingTheItemReturns400() throws Exception {
        doThrow(new DomainValidationException("income items must not have a category")).when(plans).changeItem(any());

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00","period":"YEARLY","category":"LEBENSUNTERHALT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("income items must not have a category"));
    }

    /** PB-6 */
    @Test
    void changeUnknownItemReturns404() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new BudgetItemNotFoundException(BudgetItemId.of(itemId))).when(plans).changeItem(any());

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00","period":"YEARLY"}
                                """))
                .andExpect(status().isNotFound());
    }
}
