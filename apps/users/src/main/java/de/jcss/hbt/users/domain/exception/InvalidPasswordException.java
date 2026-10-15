package de.jcss.hbt.users.domain.exception;

/** Thrown when a password does not meet the password rules. */
public class InvalidPasswordException extends DomainValidationException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
