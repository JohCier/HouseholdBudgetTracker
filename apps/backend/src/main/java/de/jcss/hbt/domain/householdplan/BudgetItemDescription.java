package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * What a {@link BudgetItem} is about, e.g. "Miete". Must not be blank and at most
 * {@value #MAX_LENGTH} characters long.
 */
@ValueObject
public record BudgetItemDescription(String value) {

    public static final int MAX_LENGTH = 255;

    public BudgetItemDescription {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("description must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("description must not be longer than %d characters".formatted(MAX_LENGTH));
        }
    }

    public static BudgetItemDescription of(String value) {
        return new BudgetItemDescription(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
