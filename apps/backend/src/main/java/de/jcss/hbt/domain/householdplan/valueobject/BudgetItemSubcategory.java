package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Locale;

/**
 * Optional, freely chosen name that groups {@link BudgetItem}s beyond their category, e.g. "Auto"
 * (UK-1). Leading and trailing whitespace is removed; what remains must not be blank and at most
 * {@value #MAX_LENGTH} characters long (UK-2). An item without a subcategory belongs to
 * {@link #OTHER} (UK-1).
 * <p>
 * Two subcategories denote the same group if their names are equal ignoring case, see
 * {@link #isSameAs} (UK-5); equals compares the exact spelling.
 */
@ValueObject
public record BudgetItemSubcategory(String value) {

    public static final int MAX_LENGTH = 50;

    /** The group of all items without a subcategory, and a predefined subcategory of its own (UK-1, UK-8). */
    public static final BudgetItemSubcategory OTHER = new BudgetItemSubcategory("Sonstiges");

    public BudgetItemSubcategory {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("subcategory must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                    "subcategory must not be longer than %d characters".formatted(MAX_LENGTH));
        }
    }

    public static BudgetItemSubcategory of(String value) {
        return new BudgetItemSubcategory(value);
    }

    /**
     * The subcategory of the given name, or {@code null} if the name is {@code null} or blank:
     * such a name means the item has no subcategory (UK-2).
     */
    public static BudgetItemSubcategory ofNullable(String value) {
        return value == null || value.isBlank() ? null : new BudgetItemSubcategory(value);
    }

    /** The group an item with the given subcategory belongs to: {@link #OTHER} for none (UK-1). */
    public static BudgetItemSubcategory groupOf(BudgetItemSubcategory subcategory) {
        return subcategory == null ? OTHER : subcategory;
    }

    /** Whether both name the same group, i.e. are equal ignoring case (UK-5). */
    public boolean isSameAs(BudgetItemSubcategory other) {
        return other != null && key().equals(other.key());
    }

    /** Whether this is the group {@link #OTHER}, whatever its spelling. */
    public boolean isOther() {
        return isSameAs(OTHER);
    }

    String key() {
        return value.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
