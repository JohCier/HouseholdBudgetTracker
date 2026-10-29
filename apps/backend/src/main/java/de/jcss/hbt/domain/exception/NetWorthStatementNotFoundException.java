package de.jcss.hbt.domain.exception;

import java.time.Year;

/**
 * Thrown when a user has no {@code NetWorthStatement} for a year, i.e. no asset or liability is recorded for it (VS-7).
 */
public class NetWorthStatementNotFoundException extends RuntimeException {

    public NetWorthStatementNotFoundException(Year year) {
        super("No net worth recorded for " + year);
    }
}
