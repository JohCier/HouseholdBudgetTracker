package com.example.householdbudgettracker.domain.exception;

/**
 * Thrown when a {@code BudgetItem} would be created in violation of a domain invariant,
 * e.g. a blank description, a non-positive amount, or a missing/unexpected category.
 */
public class InvalidBudgetItemException extends RuntimeException {

    public InvalidBudgetItemException(String message) {
        super(message);
    }
}
