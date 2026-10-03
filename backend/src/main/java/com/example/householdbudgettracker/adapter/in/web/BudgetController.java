package com.example.householdbudgettracker.adapter.in.web;

import com.example.householdbudgettracker.adapter.in.web.dto.AddExpenseRequest;
import com.example.householdbudgettracker.adapter.in.web.dto.AddIncomeRequest;
import com.example.householdbudgettracker.adapter.in.web.dto.BudgetItemIdResponse;
import com.example.householdbudgettracker.adapter.in.web.dto.BudgetItemResponse;
import com.example.householdbudgettracker.adapter.in.web.dto.BudgetOverviewResponse;
import com.example.householdbudgettracker.application.port.in.AddExpenseUseCase;
import com.example.householdbudgettracker.application.port.in.AddExpenseUseCase.AddExpenseCommand;
import com.example.householdbudgettracker.application.port.in.AddIncomeUseCase;
import com.example.householdbudgettracker.application.port.in.AddIncomeUseCase.AddIncomeCommand;
import com.example.householdbudgettracker.application.port.in.GetBudgetOverviewUseCase;
import com.example.householdbudgettracker.application.port.in.ListBudgetItemsUseCase;
import com.example.householdbudgettracker.application.port.in.RemoveBudgetItemUseCase;
import com.example.householdbudgettracker.domain.BudgetItemId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Primary (driving) web adapter exposing the budget use cases as a REST API.
 * Contains no business logic; it only translates HTTP requests/responses to and
 * from the application layer's ports.
 */
@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final AddIncomeUseCase addIncomeUseCase;
    private final AddExpenseUseCase addExpenseUseCase;
    private final RemoveBudgetItemUseCase removeBudgetItemUseCase;
    private final ListBudgetItemsUseCase listBudgetItemsUseCase;
    private final GetBudgetOverviewUseCase getBudgetOverviewUseCase;

    public BudgetController(AddIncomeUseCase addIncomeUseCase,
                             AddExpenseUseCase addExpenseUseCase,
                             RemoveBudgetItemUseCase removeBudgetItemUseCase,
                             ListBudgetItemsUseCase listBudgetItemsUseCase,
                             GetBudgetOverviewUseCase getBudgetOverviewUseCase) {
        this.addIncomeUseCase = addIncomeUseCase;
        this.addExpenseUseCase = addExpenseUseCase;
        this.removeBudgetItemUseCase = removeBudgetItemUseCase;
        this.listBudgetItemsUseCase = listBudgetItemsUseCase;
        this.getBudgetOverviewUseCase = getBudgetOverviewUseCase;
    }

    @PostMapping("/incomes")
    public ResponseEntity<BudgetItemIdResponse> addIncome(@RequestBody AddIncomeRequest request) {
        BudgetItemId id = addIncomeUseCase.addIncome(new AddIncomeCommand(request.description(), request.amount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(BudgetItemIdResponse.from(id));
    }

    @PostMapping("/expenses")
    public ResponseEntity<BudgetItemIdResponse> addExpense(@RequestBody AddExpenseRequest request) {
        BudgetItemId id = addExpenseUseCase.addExpense(
                new AddExpenseCommand(request.description(), request.amount(), request.category()));
        return ResponseEntity.status(HttpStatus.CREATED).body(BudgetItemIdResponse.from(id));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID id) {
        removeBudgetItemUseCase.removeItem(BudgetItemId.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    public List<BudgetItemResponse> listItems() {
        return listBudgetItemsUseCase.listItems().stream().map(BudgetItemResponse::from).toList();
    }

    @GetMapping("/overview")
    public BudgetOverviewResponse overview() {
        return BudgetOverviewResponse.from(getBudgetOverviewUseCase.getOverview());
    }
}
