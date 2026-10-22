package de.jcss.hbt.users.domain.exception;

/** Thrown when the new password of a user is the same as their current one (PA-4). */
public class PasswordUnchangedException extends RuntimeException {

    public PasswordUnchangedException() {
        super("The new password must differ from the current one");
    }
}
