package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;

import java.time.Instant;
import java.util.Objects;

/**
 * A household plan got copies of all items of another plan of the same user, the source (PU-11).
 *
 * @param overwritten whether the plan existed before and its items were replaced; otherwise it was just created
 */
public record HouseholdPlanCopied(HouseholdPlanId planId, HouseholdPlanId sourcePlanId, boolean overwritten,
                                  Instant timestamp) implements HouseholdPlanEvent {

    public HouseholdPlanCopied {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(sourcePlanId, "sourcePlanId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
