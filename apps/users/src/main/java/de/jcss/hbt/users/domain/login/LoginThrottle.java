package de.jcss.hbt.users.domain.login;

import de.jcss.hbt.users.domain.exception.LoginLockedException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root counting the failed sign-ins with one e-mail address (AN-10). After
 * {@value #MAX_FAILED_ATTEMPTS} failures within {@link #WINDOW}, signing in with the address is
 * locked for {@link #LOCK_DURATION}, even with the right password. It works the same for
 * addresses nobody registered, so a lock does not tell which addresses exist (AN-6).
 * <p>
 * Immutable: a failed attempt yields a new throttle. A successful sign-in ends it, i.e.
 * the application layer removes it from its repository.
 * <p>
 * Identity (equals/hashCode) is based solely on the e-mail address.
 */
@AggregateRoot
public final class LoginThrottle {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Identity
    private final EmailAddress email;
    /** Failed attempts since {@link #windowStartedAt}; zero once a lock has been set. */
    private final int failedAttempts;
    private final Instant windowStartedAt;
    private final Instant lockedUntil;

    private LoginThrottle(EmailAddress email, int failedAttempts, Instant windowStartedAt, Instant lockedUntil) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        if (failedAttempts < 0 || failedAttempts >= MAX_FAILED_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "failedAttempts must be between 0 and %d".formatted(MAX_FAILED_ATTEMPTS - 1));
        }
        if (failedAttempts > 0 && windowStartedAt == null) {
            throw new IllegalArgumentException("failed attempts need the start of their window");
        }
        this.failedAttempts = failedAttempts;
        this.windowStartedAt = windowStartedAt;
        this.lockedUntil = lockedUntil;
    }

    /** A throttle without any failed attempt yet. */
    public static LoginThrottle start(EmailAddress email) {
        return new LoginThrottle(email, 0, null, null);
    }

    /**
     * Rebuilds a stored throttle, e.g. in a repository.
     *
     * @param windowStartedAt time of the first failed attempt counted; {@code null} without one
     * @param lockedUntil     end of the latest lock; {@code null} if there never was one
     */
    public static LoginThrottle reconstitute(EmailAddress email, int failedAttempts, Instant windowStartedAt,
                                             Instant lockedUntil) {
        return new LoginThrottle(email, failedAttempts, windowStartedAt, lockedUntil);
    }

    /**
     * Checks that signing in with the address is allowed at the given time.
     *
     * @throws LoginLockedException if the address is locked then
     */
    public void ensureNotLocked(Instant now) {
        if (isLockedAt(now)) {
            throw new LoginLockedException();
        }
    }

    private boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * The throttle after one more failed attempt at the given time: counts it within the
     * current window or starts a new window, and locks the address when the attempt is
     * the {@value #MAX_FAILED_ATTEMPTS}th within {@link #WINDOW}.
     */
    public LoginThrottle afterFailedAttempt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        boolean windowOpen = windowStartedAt != null && now.isBefore(windowStartedAt.plus(WINDOW));
        int attempts = windowOpen ? failedAttempts + 1 : 1;
        Instant windowStart = windowOpen ? windowStartedAt : now;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            return new LoginThrottle(email, 0, null, now.plus(LOCK_DURATION));
        }
        return new LoginThrottle(email, attempts, windowStart, lockedUntil);
    }

    public EmailAddress email() {
        return email;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Instant windowStartedAt() {
        return windowStartedAt;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginThrottle other)) {
            return false;
        }
        return email.equals(other.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return "LoginThrottle{failedAttempts=%d, lockedUntil=%s}".formatted(failedAttempts, lockedUntil);
    }
}
