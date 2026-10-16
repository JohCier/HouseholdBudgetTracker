package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/** A user started a new, empty household plan for a year. */
public record HouseholdPlanCreated(HouseholdPlanId planId, OwnerId owner, Year year, Instant timestamp)
        implements DomainEvent {

    public HouseholdPlanCreated {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
