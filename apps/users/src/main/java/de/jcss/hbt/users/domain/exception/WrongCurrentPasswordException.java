package de.jcss.hbt.users.domain.exception;

/** Thrown when a user tries to change their password with a wrong current password (PA-2). */
public class WrongCurrentPasswordException extends RuntimeException {

    public WrongCurrentPasswordException() {
        super("The current password is wrong");
    }
}
