package de.jcss.hbt.domain.exception;

/**
 * Thrown when a {@code BudgetItem} would be created in violation of a domain invariant,
 * e.g. a blank description, a non-positive amount, or a missing category.
 */
public class InvalidBudgetItemException extends DomainValidationException {

    public InvalidBudgetItemException(String message) {
        super(message);
    }
}
