package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Instant;
import java.util.Objects;

/** The user moved items of a subcategory group of a household plan to other positions (UK-14). */
public record BudgetItemsReordered(HouseholdPlanId planId, Instant timestamp) implements HouseholdPlanEvent {

    public BudgetItemsReordered {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
