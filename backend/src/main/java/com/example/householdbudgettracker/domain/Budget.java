package com.example.householdbudgettracker.domain;

import com.example.householdbudgettracker.domain.exception.BudgetItemNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate root managing a collection of {@link BudgetItem} entities. All access to
 * and mutation of items happens exclusively through this aggregate, which is the
 * consistency boundary for adding/removing items and for deriving the
 * {@link BudgetOverview}.
 */
public final class Budget {

    private final BudgetId id;
    private final Map<BudgetItemId, BudgetItem> items = new LinkedHashMap<>();

    private Budget(BudgetId id) {
        this.id = id;
    }

    public static Budget createEmpty() {
        return new Budget(BudgetId.generate());
    }

    public BudgetId id() {
        return id;
    }

    public BudgetItemId addIncome(String description, Money amount) {
        BudgetItemId itemId = BudgetItemId.generate();
        items.put(itemId, BudgetItem.income(itemId, description, amount));
        return itemId;
    }

    public BudgetItemId addExpense(String description, Money amount, ExpenseCategory category) {
        BudgetItemId itemId = BudgetItemId.generate();
        items.put(itemId, BudgetItem.expense(itemId, description, amount, category));
        return itemId;
    }

    public void removeItem(BudgetItemId itemId) {
        if (items.remove(itemId) == null) {
            throw new BudgetItemNotFoundException(itemId);
        }
    }

    public List<BudgetItem> items() {
        return List.copyOf(items.values());
    }

    public BudgetOverview overview() {
        return BudgetOverview.of(items());
    }
}
