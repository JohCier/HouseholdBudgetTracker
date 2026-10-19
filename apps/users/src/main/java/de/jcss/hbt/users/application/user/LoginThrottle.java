package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.user.EmailAddress;

/**
 * Counts failed sign-ins per e-mail address and locks an address after too many of them
 * (AN-10). A technical safeguard of signing in, not a domain concept: the application layer
 * declares what it needs, the infrastructure decides how the attempts are kept.
 * <p>
 * Works the same for addresses nobody registered, so a lock does not tell which addresses
 * exist (AN-6).
 */
public interface LoginThrottle {

    /**
     * Counts a sign-in attempt with the address as failed, before its password is checked, and
     * locks the address when the limit is reached. Checking and counting happen in one step, so
     * attempts made at the same time cannot all get past the limit. A successful attempt is
     * taken back with {@link #reset}.
     *
     * @throws LoginLockedException if signing in with the address is locked right now; the
     *                              attempt is then not counted
     */
    void countAttempt(EmailAddress email);

    /** Forgets the failed sign-ins with the address after a successful one. */
    void reset(EmailAddress email);
}
