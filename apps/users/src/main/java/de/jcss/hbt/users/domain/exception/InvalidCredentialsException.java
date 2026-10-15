package de.jcss.hbt.users.domain.exception;

/**
 * Thrown when signing in fails. Deliberately does not tell whether the e-mail address is
 * unknown or the password is wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail address or password is wrong");
    }
}
