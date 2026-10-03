package com.example.householdbudgettracker.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of the {@link Budget} aggregate.
 */
public record BudgetId(UUID value) {

    public BudgetId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BudgetId generate() {
        return new BudgetId(UUID.randomUUID());
    }

    public static BudgetId of(UUID value) {
        return new BudgetId(value);
    }
}
