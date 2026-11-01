package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.time.Year;

/**
 * Thrown when a {@code HouseholdPlan} is referenced by an id or a year it does not exist for.
 */
public class HouseholdPlanNotFoundException extends NotFoundException {

    private static final String CODE = "plan-not-found";

    public HouseholdPlanNotFoundException(HouseholdPlanId id) {
        super(CODE, "Household plan not found: " + id.value());
    }

    public HouseholdPlanNotFoundException(Year year) {
        super(CODE, "No household plan for " + year);
    }
}
