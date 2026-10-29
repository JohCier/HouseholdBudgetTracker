package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/**
 * A user deleted one of their deleted household plans for good, with all its items (GP-17, GP-27).
 *
 * @param itemCount how many items the plan had
 */
public record HouseholdPlanPurged(HouseholdPlanId planId, Year year, int itemCount, Instant timestamp)
        implements HouseholdPlanEvent {

    public HouseholdPlanPurged {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must not be negative, but was " + itemCount);
        }
    }
}
