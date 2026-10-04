package de.jcss.hbt.adapter.in.web;

import de.jcss.hbt.application.port.in.BudgetUseCases;
import de.jcss.hbt.application.port.in.BudgetUseCases.AddExpenseCommand;
import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.BudgetOverview;
import de.jcss.hbt.domain.ExpenseCategory;
import de.jcss.hbt.domain.Money;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.InvalidMoneyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    private static final Year YEAR = Year.of(2026);
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetUseCases budget;

    @Test
    void addIncomeReturns201WithId() throws Exception {
        BudgetItemId id = BudgetItemId.generate();
        when(budget.addIncome(any())).thenReturn(id);

        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"2000.00","bookingDate":"2026-10-04"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void addExpenseMapsCategoryAndDate() throws Exception {
        when(budget.addExpense(any())).thenReturn(BudgetItemId.generate());

        mockMvc.perform(post("/api/budget/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800,"category":"LEBENSUNTERHALT","bookingDate":"2026-10-04"}
                                """))
                .andExpect(status().isCreated());

        verify(budget).addExpense(new AddExpenseCommand(
                "Miete", new BigDecimal("800"), ExpenseCategory.LEBENSUNTERHALT, DATE));
    }

    @Test
    void invalidRequestReturnsProblemDetailListingAllFields() throws Exception {
        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.bookingDate").exists())
                .andExpect(jsonPath("$.detail").value(containsString("amount")));

        verifyNoInteractions(budget);
    }

    @Test
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/budget/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":"800","category":"GIBTS_NICHT","bookingDate":"2026-10-04"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void domainValidationErrorReturns400WithDetail() throws Exception {
        when(budget.addIncome(any())).thenThrow(new InvalidMoneyException("amount must not have more than 2 decimal places"));

        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"1.00","bookingDate":"2026-10-04"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("amount must not have more than 2 decimal places"));
    }

    @Test
    void removeItemReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNoContent());

        verify(budget).removeItem(BudgetItemId.of(id));
    }

    @Test
    void removeUnknownItemReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BudgetItemNotFoundException(BudgetItemId.of(id)))
                .when(budget).removeItem(eq(BudgetItemId.of(id)));

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Budget item not found: " + id));
    }

    @Test
    void listItemsReturnsItemsOfTheYearWithAmountsAsStrings() throws Exception {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("2000")), DATE);
        when(budget.listItems(YEAR)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/budget/items").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Gehalt"))
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[0].amount").value("2000.00"))
                .andExpect(jsonPath("$[0].category").isEmpty())
                .andExpect(jsonPath("$[0].bookingDate").value("2026-10-04"));
    }

    @Test
    void listItemsRequiresYear() throws Exception {
        mockMvc.perform(get("/api/budget/items"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overviewReturnsAggregatedSummary() throws Exception {
        when(budget.getOverview(YEAR)).thenReturn(BudgetOverview.of(YEAR, List.of(
                BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("1000.00")), DATE),
                BudgetItem.expense(BudgetItemId.generate(), "ETF", Money.of(new BigDecimal("500.00")),
                        ExpenseCategory.SPAREN_INVESTIEREN, DATE)
        )));

        mockMvc.perform(get("/api/budget/overview").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalIncome").value("1000.00"))
                .andExpect(jsonPath("$.totalExpense").value("500.00"))
                .andExpect(jsonPath("$.balance").value("500.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false))
                .andExpect(jsonPath("$.categoryBreakdowns[2].category").value("SPAREN_INVESTIEREN"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].targetDirection").value("AT_LEAST"))
                .andExpect(jsonPath("$.categoryBreakdowns[2].targetMet").value(true));
    }
}
