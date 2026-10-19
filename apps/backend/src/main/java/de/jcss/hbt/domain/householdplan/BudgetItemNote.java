package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Optional free text about a {@link BudgetItem}, e.g. how its amount is made up (PN-1). Leading
 * and trailing whitespace is removed; what remains must not be blank and at most
 * {@value #MAX_LENGTH} characters long (PN-2, PN-3). Line breaks are allowed.
 */
@ValueObject
public record BudgetItemNote(String value) {

    public static final int MAX_LENGTH = 1000;

    public BudgetItemNote {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("note must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("note must not be longer than %d characters".formatted(MAX_LENGTH));
        }
    }

    /**
     * The note of the given text, or {@code null} if the text is {@code null} or blank: such
     * a text means the item has no note (PN-3).
     */
    public static BudgetItemNote ofNullable(String value) {
        return value == null || value.isBlank() ? null : new BudgetItemNote(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
