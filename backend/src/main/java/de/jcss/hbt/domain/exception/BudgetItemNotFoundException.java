package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.BudgetItemId;

/**
 * Thrown when a {@code BudgetItem} is referenced by an id that does not exist in the budget.
 */
public class BudgetItemNotFoundException extends RuntimeException {

    public BudgetItemNotFoundException(BudgetItemId id) {
        super("Budget item not found: " + id.value());
    }
}
