package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a single income or expense position of a {@link HouseholdPlan}. It
 * has no date of its own: it belongs to the year of the plan it lives in. Items are
 * immutable and only added to, changed in or removed from a plan through the plan itself:
 * new items are created by the package-private {@link #income} / {@link #expense} factories,
 * changed ones by {@link #changedTo}, which only the plan calls; stored items are rebuilt by
 * repositories via {@link #reconstitute}. All of them enforce the domain rules:
 * <ul>
 *     <li>description is required (its own rules are enforced by {@link BudgetItemDescription})</li>
 *     <li>amount must be positive</li>
 *     <li>an expense item requires a category; an income item never has one</li>
 *     <li>the note is optional for both (its own rules are enforced by {@link BudgetItemNote})</li>
 * </ul>
 * Identity (equals/hashCode) is based solely on the item id, as is appropriate for
 * a DDD entity.
 */
@Entity
public final class BudgetItem {

    @Identity
    private final BudgetItemId id;
    private final BudgetItemDescription description;
    private final Money amount;
    private final ItemType type;
    private final ExpenseCategory category;
    private final BudgetItemNote note;

    private BudgetItem(BudgetItemId id, BudgetItemDescription description, Money amount, ItemType type,
                       ExpenseCategory category, BudgetItemNote note) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = requireDescription(description);
        this.amount = requirePositive(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = category;
        this.note = note;
    }

    /** @param note {@code null} if the item has no note */
    static BudgetItem income(BudgetItemId id, BudgetItemDescription description, Money amount, BudgetItemNote note) {
        return new BudgetItem(id, description, amount, ItemType.INCOME, null, note);
    }

    /** @param note {@code null} if the item has no note */
    static BudgetItem expense(BudgetItemId id, BudgetItemDescription description, Money amount,
                              ExpenseCategory category, BudgetItemNote note) {
        if (category == null) {
            throw new InvalidBudgetItemException("category is required for expense items");
        }
        return new BudgetItem(id, description, amount, ItemType.EXPENSE, category, note);
    }

    /**
     * Rebuilds a stored item, e.g. in a repository; only meant for persistence.
     *
     * @param category the category of an expense; {@code null} for an income
     * @param note     {@code null} if the item has no note
     */
    public static BudgetItem reconstitute(BudgetItemId id, BudgetItemDescription description, Money amount,
                                          ItemType type, ExpenseCategory category, BudgetItemNote note) {
        return switch (Objects.requireNonNull(type, "type must not be null")) {
            case INCOME -> {
                if (category != null) {
                    throw new InvalidBudgetItemException("income items must not have a category");
                }
                yield income(id, description, amount, note);
            }
            case EXPENSE -> expense(id, description, amount, category, note);
        };
    }

    /**
     * Returns a copy of this item with the same id and type but the given values. The type
     * itself cannot be changed: an income must not get a category, an expense needs one.
     *
     * @param category the new category of an expense; {@code null} for an income
     * @param note     the new note; {@code null} to remove it
     */
    BudgetItem changedTo(BudgetItemDescription description, Money amount, ExpenseCategory category,
                         BudgetItemNote note) {
        return reconstitute(id, description, amount, type, category, note);
    }

    /** Whether the other item has the same description, amount, type, category and note as this one. */
    boolean hasSameValuesAs(BudgetItem other) {
        return description.equals(other.description) && amount.equals(other.amount) && type == other.type
                && Objects.equals(category, other.category) && Objects.equals(note, other.note);
    }

    private static BudgetItemDescription requireDescription(BudgetItemDescription description) {
        if (description == null) {
            throw new InvalidBudgetItemException("description is required");
        }
        return description;
    }

    private static Money requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidBudgetItemException("amount must be positive");
        }
        return amount;
    }

    boolean hasId(BudgetItemId itemId) {
        return id.equals(itemId);
    }

    /** Returns the given summary with this item's amount added as income or as expense of its category. */
    public BudgetSummary addTo(BudgetSummary summary) {
        return switch (type) {
            case INCOME -> summary.withIncome(amount);
            case EXPENSE -> summary.withExpense(category, amount);
        };
    }

    public BudgetItemId id() {
        return id;
    }

    public BudgetItemDescription description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public ItemType type() {
        return type;
    }

    public Optional<ExpenseCategory> category() {
        return Optional.ofNullable(category);
    }

    public Optional<BudgetItemNote> note() {
        return Optional.ofNullable(note);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BudgetItem other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BudgetItem{id=%s, description='%s', amount=%s, type=%s, category=%s}"
                .formatted(id, description, amount, type, category);
    }
}
