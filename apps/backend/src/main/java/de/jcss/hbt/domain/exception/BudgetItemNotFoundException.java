package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.householdplan.BudgetItemId;

/**
 * Thrown when a {@code BudgetItem} is referenced by an id that does not exist in the household plan.
 */
public class BudgetItemNotFoundException extends RuntimeException {

    public BudgetItemNotFoundException(BudgetItemId id) {
        super("Budget item not found: " + id.value());
    }
}
