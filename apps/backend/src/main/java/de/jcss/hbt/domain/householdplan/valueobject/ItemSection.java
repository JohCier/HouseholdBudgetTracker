package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * Where an item is listed among the entries: the incomes, or the expenses of one category (UK-9,
 * UK-10). Within a section, the items are grouped by subcategory (UK-11).
 *
 * @param category the category of an expense section; {@code null} for the incomes
 */
@ValueObject
public record ItemSection(ItemType type, ExpenseCategory category) {

    public static final ItemSection INCOME = new ItemSection(ItemType.INCOME, null);

    public ItemSection {
        Objects.requireNonNull(type, "type must not be null");
        if (type == ItemType.INCOME && category != null) {
            throw new DomainValidationException("income items must not have a category");
        }
        if (type == ItemType.EXPENSE && category == null) {
            throw new DomainValidationException("category is required for expense items");
        }
    }

    public static ItemSection expenses(ExpenseCategory category) {
        return new ItemSection(ItemType.EXPENSE, category);
    }
}
