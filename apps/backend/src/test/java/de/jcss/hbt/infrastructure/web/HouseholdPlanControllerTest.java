package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requests are sent on behalf of a signed-in user ({@link #OWNER}) unless stated otherwise;
 * reading the session token from its cookie is covered by {@code HouseholdPlanIntegrationTest}.
 */
@WebMvcTest(HouseholdPlanController.class)
@Import(SecurityConfig.class)
class HouseholdPlanControllerTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final UUID PLAN = PLAN_ID.value();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc anonymous;

    @MockitoBean
    private HouseholdPlanApplicationService plans;

    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject(OWNER.value().toString()))))
                .build();
    }

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

    @Test
    void createPlanReturns201WithId() throws Exception {
        when(plans.createPlan(new CreateHouseholdPlanCommand(OWNER, Year.of(2026)))).thenReturn(PLAN_ID);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PLAN.toString()));
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

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Miete"),
                Money.of(new BigDecimal("800")), ExpenseCategory.LEBENSUNTERHALT, null));
    }

    /** PN-1, PN-3 */
    @Test
    void addItemPassesTheNoteAndTreatsABlankOneAsNone() throws Exception {
        when(plans.addExpense(any())).thenReturn(BudgetItemId.generate());
        when(plans.addIncome(any())).thenReturn(BudgetItemId.generate());

        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800,"category":"LEBENSUNTERHALT","note":"Warmmiete"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"note":"  "}
                                """))
                .andExpect(status().isCreated());

        verify(plans).addExpense(new AddExpenseCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Miete"),
                Money.of(new BigDecimal("800")), ExpenseCategory.LEBENSUNTERHALT, new BudgetItemNote("Warmmiete")));
        verify(plans).addIncome(new AddIncomeCommand(OWNER, PLAN_ID, BudgetItemDescription.of("Gehalt"),
                Money.of(new BigDecimal("2000")), null));
    }

    /** PN-2 */
    @Test
    void tooLongNoteReturns400() throws Exception {
        String note = "x".repeat(BudgetItemNote.MAX_LENGTH + 1);

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"note":"%s"}
                                """.formatted(note)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.note").exists());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000,"note":"%s"}
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
                                {"description":"Gehalt","amount":"2100","note":"netto"}
                                """))
                .andExpect(status().isNoContent());

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), Money.of(new BigDecimal("2100")), null, new BudgetItemNote("netto")));
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
        when(plans.addIncome(any())).thenThrow(new DomainValidationException("amount must not have more than 2 decimal places"));

        mockMvc.perform(post("/api/plans/{planId}/incomes", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00"}
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
                                {"description":"Gehalt","amount":"1.00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:plan-not-found"))
                .andExpect(jsonPath("$.detail").value("Household plan not found: " + PLAN));
    }

    @Test
    void removeItemReturns204() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", PLAN, itemId))
                .andExpect(status().isNoContent());

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

    /** PB-1 */
    @Test
    void changeItemReturns204AndMapsAllValues() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"900.00","category":"SPAREN_INVESTIEREN"}
                                """))
                .andExpect(status().isNoContent());

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Miete"), Money.of(new BigDecimal("900.00")), ExpenseCategory.SPAREN_INVESTIEREN, null));
    }

    /** PB-1 */
    @Test
    void changeItemWithoutCategoryPassesNone() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2100"}
                                """))
                .andExpect(status().isNoContent());

        verify(plans).changeItem(new ChangeBudgetItemCommand(OWNER, PLAN_ID, BudgetItemId.of(itemId),
                BudgetItemDescription.of("Gehalt"), Money.of(new BigDecimal("2100")), null, null));
    }

    /** PB-3 */
    @Test
    void invalidChangeRequestReturns400() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", PLAN, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":" ","amount":"10.005"}
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
                                {"description":"Gehalt","amount":"1.00","category":"LEBENSUNTERHALT"}
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
                                {"description":"Gehalt","amount":"1.00"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void overviewOfAYearReturnsTotalsBreakdownsAndItems() throws Exception {
        BudgetItemId itemId = BudgetItemId.generate();
        when(plans.getOverview(OWNER, Year.of(2026))).thenReturn(HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(
                new OverviewItem(itemId, BudgetItemDescription.of("ETF"), Money.of(new BigDecimal("500")),
                        ItemType.EXPENSE, ExpenseCategory.SPAREN_INVESTIEREN, new BudgetItemNote("ETF-Sparplan")),
                new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                        Money.of(new BigDecimal("1000")), ItemType.INCOME, null, null))));

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
