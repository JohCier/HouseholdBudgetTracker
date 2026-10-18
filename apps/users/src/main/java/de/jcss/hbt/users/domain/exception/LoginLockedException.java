package de.jcss.hbt.users.domain.exception;

/**
 * Thrown when signing in with an address that is locked after too many failed attempts.
 * Deliberately does not tell whether the address is registered.
 */
public class LoginLockedException extends RuntimeException {

    public LoginLockedException() {
        super("Too many failed sign-in attempts with this e-mail address, try again later");
    }
}
