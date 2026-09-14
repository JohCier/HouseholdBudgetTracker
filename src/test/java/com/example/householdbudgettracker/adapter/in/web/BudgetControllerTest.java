package com.example.householdbudgettracker.adapter.in.web;

import com.example.householdbudgettracker.application.port.in.AddExpenseUseCase;
import com.example.householdbudgettracker.application.port.in.AddIncomeUseCase;
import com.example.householdbudgettracker.application.port.in.GetBudgetOverviewUseCase;
import com.example.householdbudgettracker.application.port.in.ListBudgetItemsUseCase;
import com.example.householdbudgettracker.application.port.in.RemoveBudgetItemUseCase;
import com.example.householdbudgettracker.domain.BudgetItem;
import com.example.householdbudgettracker.domain.BudgetItemId;
import com.example.householdbudgettracker.domain.BudgetOverview;
import com.example.householdbudgettracker.domain.ExpenseCategory;
import com.example.householdbudgettracker.domain.Money;
import com.example.householdbudgettracker.domain.exception.BudgetItemNotFoundException;
import com.example.householdbudgettracker.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddIncomeUseCase addIncomeUseCase;

    @MockitoBean
    private AddExpenseUseCase addExpenseUseCase;

    @MockitoBean
    private RemoveBudgetItemUseCase removeBudgetItemUseCase;

    @MockitoBean
    private ListBudgetItemsUseCase listBudgetItemsUseCase;

    @MockitoBean
    private GetBudgetOverviewUseCase getBudgetOverviewUseCase;

    @Test
    void addIncomeReturns201WithId() throws Exception {
        BudgetItemId id = BudgetItemId.generate();
        when(addIncomeUseCase.addIncome(any())).thenReturn(id);

        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":2000.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void addIncomeWithInvalidDataReturns400() throws Exception {
        when(addIncomeUseCase.addIncome(any())).thenThrow(new InvalidBudgetItemException("amount must be positive"));

        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("amount must be positive"));
    }

    @Test
    void addExpenseReturns201WithId() throws Exception {
        BudgetItemId id = BudgetItemId.generate();
        when(addExpenseUseCase.addExpense(any())).thenReturn(id);

        mockMvc.perform(post("/api/budget/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Miete","amount":800.00,"category":"LEBENSUNTERHALT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void removeItemReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeUnknownItemReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BudgetItemNotFoundException(BudgetItemId.of(id)))
                .when(removeBudgetItemUseCase).removeItem(eq(BudgetItemId.of(id)));

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void listItemsReturnsAllItems() throws Exception {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("2000.00")));
        when(listBudgetItemsUseCase.listItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/budget/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Gehalt"))
                .andExpect(jsonPath("$[0].type").value("INCOME"));
    }

    @Test
    void overviewReturnsAggregatedSummary() throws Exception {
        List<BudgetItem> items = List.of(
                BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal("1000.00"))),
                BudgetItem.expense(BudgetItemId.generate(), "Miete", Money.of(new BigDecimal("500.00")), ExpenseCategory.LEBENSUNTERHALT)
        );
        when(getBudgetOverviewUseCase.getOverview()).thenReturn(BudgetOverview.of(items));

        mockMvc.perform(get("/api/budget/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpense").value(500.00))
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false));
    }
}
