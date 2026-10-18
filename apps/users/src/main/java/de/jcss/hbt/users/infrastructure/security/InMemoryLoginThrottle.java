package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.application.user.LoginLockedException;
import de.jcss.hbt.users.application.user.LoginThrottle;
import de.jcss.hbt.users.domain.user.EmailAddress;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the failed sign-ins in memory (AN-10): after {@value #MAX_FAILED_ATTEMPTS} failures
 * within {@link #WINDOW}, signing in with the address is locked for {@link #LOCK_DURATION}.
 * <p>
 * The counts belong to this instance and are lost on restart, which is fine for a single
 * instance; several instances would need a shared store.
 */
@Component
public class InMemoryLoginThrottle implements LoginThrottle {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<EmailAddress, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryLoginThrottle(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void ensureNotLocked(EmailAddress email) {
        Attempts current = attempts.get(email);
        if (current != null && current.isLockedAt(clock.instant())) {
            throw new LoginLockedException();
        }
    }

    @Override
    public void recordFailure(EmailAddress email) {
        Instant now = clock.instant();
        attempts.values().removeIf(entry -> entry.isExpiredAt(now));
        attempts.compute(email, (_, current) -> current == null ? Attempts.first(now) : current.afterFailure(now));
    }

    @Override
    public void reset(EmailAddress email) {
        attempts.remove(email);
    }

    /**
     * Failed attempts since {@code windowStartedAt}, or a lock until {@code lockedUntil}
     * (then without counted attempts).
     */
    private record Attempts(int count, Instant windowStartedAt, Instant lockedUntil) {

        static Attempts first(Instant now) {
            return new Attempts(1, now, null);
        }

        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        /** Neither locked nor counting anymore, so it can be forgotten. */
        boolean isExpiredAt(Instant now) {
            return !isLockedAt(now) && (windowStartedAt == null || !now.isBefore(windowStartedAt.plus(WINDOW)));
        }

        Attempts afterFailure(Instant now) {
            boolean windowOpen = windowStartedAt != null && now.isBefore(windowStartedAt.plus(WINDOW));
            int newCount = windowOpen ? count + 1 : 1;
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                return new Attempts(0, null, now.plus(LOCK_DURATION));
            }
            return new Attempts(newCount, windowOpen ? windowStartedAt : now, lockedUntil);
        }
    }
}
