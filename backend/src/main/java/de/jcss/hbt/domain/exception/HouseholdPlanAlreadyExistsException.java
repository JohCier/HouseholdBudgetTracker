package de.jcss.hbt.domain.exception;

import java.time.Year;

/**
 * Thrown when a {@code HouseholdPlan} would be created for a year that already has one.
 */
public class HouseholdPlanAlreadyExistsException extends RuntimeException {

    public HouseholdPlanAlreadyExistsException(Year year) {
        super("A household plan for %s already exists".formatted(year));
    }
}
