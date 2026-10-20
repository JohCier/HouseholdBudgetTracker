package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/**
 * A user deleted one of their household plans with all its items (PL-6).
 *
 * @param itemCount how many items the plan had when it was deleted
 */
public record HouseholdPlanDeleted(HouseholdPlanId planId, Year year, int itemCount, Instant timestamp)
        implements HouseholdPlanEvent {

    public HouseholdPlanDeleted {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must not be negative, but was " + itemCount);
        }
    }
}
