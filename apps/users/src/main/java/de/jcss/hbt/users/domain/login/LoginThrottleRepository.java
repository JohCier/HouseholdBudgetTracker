package de.jcss.hbt.users.domain.login;

import de.jcss.hbt.users.domain.user.EmailAddress;

import java.util.Optional;

/**
 * Collection of all {@link LoginThrottle} aggregates. A throttle exists from the first failed
 * attempt with an address until the next successful sign-in with it.
 */
public interface LoginThrottleRepository {

    /**
     * The throttle of the address, if it has one. Holds back other sign-ins with the same
     * address until the transaction ends, so concurrent failed attempts are all counted.
     */
    Optional<LoginThrottle> findByEmail(EmailAddress email);

    /**
     * Stores the throttle, whether it is new or an updated one: throttles come and go with
     * failed attempts, so there is no separate {@code add}.
     */
    void save(LoginThrottle throttle);

    /** Forgets the failed attempts with the address, e.g. after a successful sign-in. */
    void remove(EmailAddress email);
}
