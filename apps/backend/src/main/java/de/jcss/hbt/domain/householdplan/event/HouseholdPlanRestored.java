package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/** A user restored one of their deleted household plans, which is active again (GP-11, GP-27). */
public record HouseholdPlanRestored(HouseholdPlanId planId, Year year, Instant timestamp) implements HouseholdPlanEvent {

    public HouseholdPlanRestored {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
