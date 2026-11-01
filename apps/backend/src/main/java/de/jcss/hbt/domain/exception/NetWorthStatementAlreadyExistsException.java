package de.jcss.hbt.domain.exception;

import java.time.Year;

/**
 * Thrown when a {@code NetWorthStatement} would be created for, or copied without permission over, a year that already has one (VS-16).
 */
public class NetWorthStatementAlreadyExistsException extends AlreadyExistsException {

    private static final String CODE = "net-worth-already-exists";

    public NetWorthStatementAlreadyExistsException(Year year) {
        super(CODE, "Net worth for %s is already recorded".formatted(year));
    }
}
