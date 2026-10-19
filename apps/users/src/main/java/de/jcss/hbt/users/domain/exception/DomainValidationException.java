package de.jcss.hbt.users.domain.exception;

/**
 * Thrown when a value object or entity rejects invalid input, e.g. a
 * malformed e-mail address. The message tells which rule was violated; adapters treat it as
 * a client error.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
