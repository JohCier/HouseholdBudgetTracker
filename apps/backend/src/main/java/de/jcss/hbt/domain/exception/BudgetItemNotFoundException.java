package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;

/**
 * Thrown when a {@code BudgetItem} is referenced by an id that does not exist in the household plan.
 */
public class BudgetItemNotFoundException extends NotFoundException {

    private static final String CODE = "item-not-found";

    public BudgetItemNotFoundException(BudgetItemId id) {
        super(CODE, "Budget item not found: " + id.value());
    }
}
