package de.jcss.hbt.domain.exception;

import java.time.Year;

/**
 * Thrown when a {@code HouseholdPlan} would be created for a year outside the supported range.
 */
public class InvalidPlanYearException extends DomainValidationException {

    public InvalidPlanYearException(Year year, int firstYear, int lastYear) {
        super("year must be between %d and %d, but was %s".formatted(firstYear, lastYear, year));
    }
}
