package com.example.householdbudgettracker.application.port.in;

import com.example.householdbudgettracker.domain.BudgetItemId;
import com.example.householdbudgettracker.domain.ExpenseCategory;

import java.math.BigDecimal;

/**
 * Inbound (driving) port for adding an expense item, assigned to a category, to the budget.
 */
public interface AddExpenseUseCase {

    BudgetItemId addExpense(AddExpenseCommand command);

    record AddExpenseCommand(String description, BigDecimal amount, ExpenseCategory category) {
    }
}
