package de.jcss.hbt.users.domain.exception;

/** Thrown when a value is not a usable e-mail address. */
public class InvalidEmailAddressException extends DomainValidationException {

    public InvalidEmailAddressException(String message) {
        super(message);
    }
}
