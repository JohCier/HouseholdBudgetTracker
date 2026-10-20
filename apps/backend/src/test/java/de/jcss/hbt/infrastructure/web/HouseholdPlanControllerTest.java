package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.AddedBudgetItem;
import de.jcss.hbt.application.householdplan.CreatedHouseholdPlan;
import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderBudgetItemsCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderSubcategoriesCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
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
import java.util.Map;
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
    private static final HouseholdPlanOverview EMPTY_OVERVIEW = HouseholdPlanOverview.of(PLAN_ID, Year.of(2026), List.of(), Map.of());

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
        when(plans.addIncome(any())).thenReturn(new AddedBudgetItem(BudgetItemId.generate(), EMPTY_OVERVIEW));
        when(plans.addExpense(any())).thenReturn(new AddedBudgetItem(BudgetItemId.generate(), EMPTY_OVERVIEW));
        when(plans.changeItem(any())).thenReturn(EMPTY_OVERVIEW);
        when(plans.removeItem(any())).thenReturn(EMPTY_OVERVIEW);
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
                .andExpect(jsonPath("$.expenseCategories[0].groups[0].total").value("400.00"));
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
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/plans/{planId}/expenses", PLAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"800","period":"YEARLY","category":"GIBTS_NICHT"}
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

    private static ItemAmount yearly(String value) {
        return ItemAmount.yearly(Money.of(new BigDecimal(value)));
    }
}
