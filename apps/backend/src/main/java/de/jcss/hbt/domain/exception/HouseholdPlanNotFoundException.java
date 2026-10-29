package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Year;

/**
 * Thrown when a {@code HouseholdPlan} is referenced by an id or a year it does not exist for.
 */
public class HouseholdPlanNotFoundException extends RuntimeException {

    public HouseholdPlanNotFoundException(HouseholdPlanId id) {
        super("Household plan not found: " + id.value());
    }

    public HouseholdPlanNotFoundException(Year year) {
        super("No household plan for " + year);
    }
}
