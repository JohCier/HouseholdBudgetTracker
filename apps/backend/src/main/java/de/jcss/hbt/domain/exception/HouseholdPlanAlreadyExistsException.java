package de.jcss.hbt.domain.exception;

import java.time.Year;

/**
 * Thrown when a {@code HouseholdPlan} would be created for a year that already has one.
 */
public class HouseholdPlanAlreadyExistsException extends AlreadyExistsException {

    private static final String CODE = "plan-already-exists";

    public HouseholdPlanAlreadyExistsException(Year year) {
        super(CODE, "A household plan for %s already exists".formatted(year));
    }
}
