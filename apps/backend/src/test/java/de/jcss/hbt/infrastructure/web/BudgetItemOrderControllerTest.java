package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.command.ReorderBudgetItemsCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderSubcategoriesCommand;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reordering budget items and subcategories.
 */
class BudgetItemOrderControllerTest extends HouseholdPlanControllerTestSupport {

    /** UK-14 */
    @Test
    void reorderItemsReturns200WithOverview() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(plans.reorderItems(any())).thenReturn(EMPTY_OVERVIEW);

        mockMvc.perform(put("/api/plans/{planId}/item-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":["%s","%s"]}
                                """.formatted(second, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));

        verify(plans).reorderItems(new ReorderBudgetItemsCommand(OWNER, PLAN_ID,
                List.of(BudgetItemId.of(second), BudgetItemId.of(first))));
    }

    /** UK-14 */
    @Test
    void reorderItemsWithoutItemsReturns400() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/item-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.itemIds").exists());

        verifyNoInteractions(plans);
    }

    /** UK-14, UK-15 */
    @Test
    void reorderSubcategoriesReturns200WithOverview() throws Exception {
        when(plans.reorderSubcategories(any())).thenReturn(EMPTY_OVERVIEW);

        mockMvc.perform(put("/api/plans/{planId}/subcategory-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","category":"LEBENSUNTERHALT","subcategories":["Miete","Auto"]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/plans/{planId}/subcategory-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"INCOME","subcategories":["Sonstiges","Gehalt"]}
                                """))
                .andExpect(status().isOk());

        verify(plans).reorderSubcategories(new ReorderSubcategoriesCommand(OWNER, PLAN_ID,
                ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT),
                List.of(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto"))));
        verify(plans).reorderSubcategories(new ReorderSubcategoriesCommand(OWNER, PLAN_ID, ItemSection.INCOME,
                List.of(BudgetItemSubcategory.OTHER, BudgetItemSubcategory.of("Gehalt"))));
    }

    /** UK-14 */
    @Test
    void reorderSubcategoriesOfAnIncomeCategoryReturns400() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/subcategory-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"INCOME","category":"LEBENSUNTERHALT","subcategories":["Gehalt"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"));

        verifyNoInteractions(plans);
    }

    /** UK-15, HP-4 */
    @Test
    void reorderInAPlanOfSomeoneElseReturns404() throws Exception {
        when(plans.reorderSubcategories(any())).thenThrow(new HouseholdPlanNotFoundException(PLAN_ID));

        mockMvc.perform(put("/api/plans/{planId}/subcategory-order", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"INCOME","subcategories":["Gehalt"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-not-found"));
    }
}
