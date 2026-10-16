package de.jcss.hbt.domain.householdplan;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link BudgetItem} entity within the {@link HouseholdPlan} aggregate.
 */
@ValueObject
public record BudgetItemId(UUID value) {

    public BudgetItemId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BudgetItemId generate() {
        return new BudgetItemId(UUID.randomUUID());
    }

    public static BudgetItemId of(UUID value) {
        return new BudgetItemId(value);
    }
}
