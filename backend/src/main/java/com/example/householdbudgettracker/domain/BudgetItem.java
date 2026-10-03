package com.example.householdbudgettracker.domain;

import com.example.householdbudgettracker.domain.exception.InvalidBudgetItemException;

import java.util.Objects;
import java.util.Optional;

/**
 * Entity within the {@link Budget} aggregate representing a single income or expense
 * position. Only ever created through the {@link #income} / {@link #expense} factory
 * methods, which enforce the domain rules:
 * <ul>
 *     <li>description must not be blank</li>
 *     <li>amount must be positive</li>
 *     <li>an expense item requires a category; an income item must not have one</li>
 * </ul>
 * Identity (equals/hashCode) is based solely on the item id, as is appropriate for
 * a DDD entity.
 */
public final class BudgetItem {

    private final BudgetItemId id;
    private final String description;
    private final Money amount;
    private final ItemType type;
    private final ExpenseCategory category;

    private BudgetItem(BudgetItemId id, String description, Money amount, ItemType type, ExpenseCategory category) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = requireNonBlank(description);
        this.amount = requirePositive(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = category;
    }

    public static BudgetItem income(BudgetItemId id, String description, Money amount) {
        return new BudgetItem(id, description, amount, ItemType.INCOME, null);
    }

    public static BudgetItem expense(BudgetItemId id, String description, Money amount, ExpenseCategory category) {
        if (category == null) {
            throw new InvalidBudgetItemException("category is required for expense items");
        }
        return new BudgetItem(id, description, amount, ItemType.EXPENSE, category);
    }

    private static String requireNonBlank(String description) {
        if (description == null || description.isBlank()) {
            throw new InvalidBudgetItemException("description must not be blank");
        }
        return description;
    }

    private static Money requirePositive(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new InvalidBudgetItemException("amount must be positive");
        }
        return amount;
    }

    public BudgetItemId id() {
        return id;
    }

    public String description() {
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
