package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainEvent;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/** A new, empty household plan was started for a year. */
public record HouseholdPlanCreated(HouseholdPlanId planId, Year year, Instant occurredAt) implements DomainEvent {

    public HouseholdPlanCreated {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
