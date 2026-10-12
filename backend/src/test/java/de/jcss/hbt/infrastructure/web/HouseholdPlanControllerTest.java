package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.exception.InvalidMoneyException;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(HouseholdPlanController.class)
class HouseholdPlanControllerTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final UUID PLAN = PLAN_ID.value();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdPlanApplicationService plans;

    @Test
    void createPlanReturns201WithId() throws Exception {
        when(plans.createPlan(new CreateHouseholdPlanCommand(Year.of(2026)))).thenReturn(PLAN_ID);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));
    }

    @Test
    void createPlanForExistingYearReturns409() throws Exception {
        when(plans.createPlan(any())).thenThrow(new HouseholdPlanAlreadyExistsException(Year.of(2026)));

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A household plan for 2026 already exists"));
    }

    @Test
    void createPlanRequiresYear() throws Exception {
        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.year").exists());

        verifyNoInteractions(plans);
    }

    @Test
    void addIncomeReturns201WithId() throws Exception {
        BudgetItemId id = BudgetItemId.generate();
        when(plans.addIncome(any())).thenReturn(id);

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void addExpenseMapsPlanAndCategory() throws Exception {
        when(plans.addExpense(any())).thenReturn(BudgetItemId.generate());

        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800,"category":"LEBENSUNTERHALT"}
                                """))
                .andExpect(status().isCreated());

        verify(plans).addExpense(new AddExpenseCommand(PLAN_ID, BudgetItemDescription.of("Miete"),
                Money.of(new BigDecimal("800")), ExpenseCategory.LEBENSUNTERHALT));
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

    @Test
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"800","category":"GIBTS_NICHT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void domainValidationErrorReturns400WithDetail() throws Exception {
        when(plans.addIncome(any())).thenThrow(new InvalidMoneyException("amount must not have more than 2 decimal places"));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("amount must not have more than 2 decimal places"));
    }

    @Test
    void unknownPlanReturns404() throws Exception {
        when(plans.addIncome(any())).thenThrow(new HouseholdPlanNotFoundException(PLAN_ID));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Household plan not found: " + PLAN));
    }

    @Test
    void removeItemReturns204() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", PLAN, itemId))
                .andExpect(status().isNoContent());

        verify(plans).removeItem(new RemoveBudgetItemCommand(PLAN_ID, BudgetItemId.of(itemId)));
    }

    @Test
    void removeUnknownItemReturns404() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new BudgetItemNotFoundException(BudgetItemId.of(itemId))).when(plans).removeItem(any());

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", PLAN, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Budget item not found: " + itemId));
    }

    @Test
    void overviewOfAYearReturnsTotalsBreakdownsAndItems() throws Exception {
        BudgetItemId itemId = BudgetItemId.generate();
        when(plans.getOverview(Year.of(2026))).thenReturn(HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(itemId, BudgetItemDescription.of("ETF"), Money.of(new BigDecimal("500")),
                        ItemType.EXPENSE, ExpenseCategory.SPAREN_INVESTIEREN),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        Money.of(new BigDecimal("1000")), ItemType.INCOME, null))));

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
                .andExpect(jsonPath("$.items[0].id").value(itemId.value().toString()))
                .andExpect(jsonPath("$.items[0].amount").value("500.00"))
                .andExpect(jsonPath("$.items[0].category").value("SPAREN_INVESTIEREN"))
                .andExpect(jsonPath("$.items[1].type").value("INCOME"))
                .andExpect(jsonPath("$.items[1].category").isEmpty());
    }

    @Test
    void overviewOfAYearWithoutPlanReturns404() throws Exception {
        when(plans.getOverview(Year.of(2030))).thenThrow(new HouseholdPlanNotFoundException(Year.of(2030)));

        mockMvc.perform(get("/api/plans").param("year", "2030"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No household plan for 2030"));
    }

    @Test
    void overviewRejectsYearOutOfRange() throws Exception {
        mockMvc.perform(get("/api/plans").param("year", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/plans").param("year", "10000"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(plans);
    }

    @Test
    void overviewRequiresYear() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(plans);
    }
}
