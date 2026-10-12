package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import de.jcss.hbt.domain.shared.DomainEntity;

import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a single income or expense position of a {@link HouseholdPlan}. It
 * has no date of its own: it belongs to the year of the plan it lives in. Items are
 * immutable and only added to or removed from a plan through the plan itself; methods
 * only the plan may call are package-private.
 * Only ever created through the {@link #income} / {@link #expense} factory methods,
 * which enforce the domain rules:
 * <ul>
 *     <li>description is required (its own rules are enforced by {@link BudgetItemDescription})</li>
 *     <li>amount must be positive</li>
 *     <li>an expense item requires a category; an income item never has one</li>
 * </ul>
 * Identity (equals/hashCode) is based solely on the item id, as is appropriate for
 * a DDD entity.
 */
@DomainEntity
public final class BudgetItem {

    private final BudgetItemId id;
    private final BudgetItemDescription description;
    private final Money amount;
    private final ItemType type;
    private final ExpenseCategory category;

    private BudgetItem(BudgetItemId id, BudgetItemDescription description, Money amount, ItemType type,
                       ExpenseCategory category) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = requireDescription(description);
        this.amount = requirePositive(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = category;
    }

    public static BudgetItem income(BudgetItemId id, BudgetItemDescription description, Money amount) {
        return new BudgetItem(id, description, amount, ItemType.INCOME, null);
    }

    public static BudgetItem expense(BudgetItemId id, BudgetItemDescription description, Money amount,
                                     ExpenseCategory category) {
        if (category == null) {
            throw new InvalidBudgetItemException("category is required for expense items");
        }
        return new BudgetItem(id, description, amount, ItemType.EXPENSE, category);
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
