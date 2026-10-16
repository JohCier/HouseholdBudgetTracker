package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.TimestampedDomainEvent;

/** A domain event about a change to one household plan. */
public interface HouseholdPlanEvent extends TimestampedDomainEvent {

    /** The plan that was changed. */
    HouseholdPlanId planId();
}
