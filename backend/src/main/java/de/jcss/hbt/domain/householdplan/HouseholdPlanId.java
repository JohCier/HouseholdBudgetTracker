package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.shared.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link HouseholdPlan} aggregate.
 */
@ValueObject
public record HouseholdPlanId(UUID value) {

    public HouseholdPlanId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static HouseholdPlanId generate() {
        return new HouseholdPlanId(UUID.randomUUID());
    }

    public static HouseholdPlanId of(UUID value) {
        return new HouseholdPlanId(value);
    }
}
