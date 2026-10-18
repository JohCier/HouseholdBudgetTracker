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
     * @throws LoginLockedException if signing in with the address is locked right now
     */
    void ensureNotLocked(EmailAddress email);

    /** Counts a failed sign-in with the address, locking it when the limit is reached. */
    void recordFailure(EmailAddress email);

    /** Forgets the failed sign-ins with the address after a successful one. */
    void reset(EmailAddress email);
}
