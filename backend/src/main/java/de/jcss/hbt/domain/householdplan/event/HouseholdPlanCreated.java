package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainEvent;

import java.time.Year;

/** A new, empty household plan was started for a year. */
public record HouseholdPlanCreated(HouseholdPlanId planId, Year year) implements DomainEvent {
}
