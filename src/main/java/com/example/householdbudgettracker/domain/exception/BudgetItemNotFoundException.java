package com.example.householdbudgettracker.domain.exception;

import com.example.householdbudgettracker.domain.BudgetItemId;

/**
 * Thrown when a {@code BudgetItem} is referenced by an id that does not exist in the budget.
 */
public class BudgetItemNotFoundException extends RuntimeException {

    public BudgetItemNotFoundException(BudgetItemId id) {
        super("Budget item not found: " + id.value());
    }
}
