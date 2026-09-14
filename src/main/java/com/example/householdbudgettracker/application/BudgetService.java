package com.example.householdbudgettracker.application;

import com.example.householdbudgettracker.application.port.in.AddExpenseUseCase;
import com.example.householdbudgettracker.application.port.in.AddIncomeUseCase;
import com.example.householdbudgettracker.application.port.in.GetBudgetOverviewUseCase;
import com.example.householdbudgettracker.application.port.in.ListBudgetItemsUseCase;
import com.example.householdbudgettracker.application.port.in.RemoveBudgetItemUseCase;
import com.example.householdbudgettracker.application.port.out.BudgetRepository;
import com.example.householdbudgettracker.domain.Budget;
import com.example.householdbudgettracker.domain.BudgetItem;
import com.example.householdbudgettracker.domain.BudgetItemId;
import com.example.householdbudgettracker.domain.BudgetOverview;
import com.example.householdbudgettracker.domain.Money;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the budget use cases against the {@link Budget} aggregate, delegating
 * all domain rules to the aggregate itself. Contains no business logic of its own.
 */
@Service
public class BudgetService implements AddIncomeUseCase, AddExpenseUseCase, RemoveBudgetItemUseCase,
        ListBudgetItemsUseCase, GetBudgetOverviewUseCase {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = Objects.requireNonNull(budgetRepository);
    }

    @Override
    public BudgetItemId addIncome(AddIncomeCommand command) {
        Budget budget = budgetRepository.load();
        BudgetItemId itemId = budget.addIncome(command.description(), Money.of(command.amount()));
        budgetRepository.save(budget);
        return itemId;
    }

    @Override
    public BudgetItemId addExpense(AddExpenseCommand command) {
        Budget budget = budgetRepository.load();
        BudgetItemId itemId = budget.addExpense(command.description(), Money.of(command.amount()), command.category());
        budgetRepository.save(budget);
        return itemId;
    }

    @Override
    public void removeItem(BudgetItemId id) {
        Budget budget = budgetRepository.load();
        budget.removeItem(id);
        budgetRepository.save(budget);
    }

    @Override
    public List<BudgetItem> listItems() {
        return budgetRepository.load().items();
    }

    @Override
    public BudgetOverview getOverview() {
        return budgetRepository.load().overview();
    }
}
