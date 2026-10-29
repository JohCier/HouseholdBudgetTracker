package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Instant;
import java.util.Objects;

/** The user moved subcategory groups of a household plan to other positions (UK-14). */
public record SubcategoriesReordered(HouseholdPlanId planId, Instant timestamp) implements HouseholdPlanEvent {

    public SubcategoriesReordered {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
