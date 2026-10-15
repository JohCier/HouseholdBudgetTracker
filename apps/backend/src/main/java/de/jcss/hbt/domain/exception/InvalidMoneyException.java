package de.jcss.hbt.domain.exception;

/**
 * Thrown when a monetary amount cannot be represented exactly, e.g. because it has
 * more decimal places than cents allow.
 */
public class InvalidMoneyException extends DomainValidationException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
