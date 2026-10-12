package de.jcss.hbt.domain.exception;

/**
 * Thrown when a percentage cannot be represented exactly, e.g. because it has more
 * decimal places than allowed.
 */
public class InvalidPercentageException extends DomainValidationException {

    public InvalidPercentageException(String message) {
        super(message);
    }
}
