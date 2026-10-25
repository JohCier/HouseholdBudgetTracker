package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.application.user.LoginLockedException;
import de.jcss.hbt.users.application.user.LoginThrottle;
import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the failed sign-ins in memory (AN-10): after {@value #MAX_FAILED_ATTEMPTS} failures
 * within {@link #WINDOW}, signing in with the address is locked for {@link #LOCK_DURATION}.
 * Attempts count as failures from the moment they start; the lock therefore already applies
 * while the attempt that reached the limit is still being checked, and is lifted again by
 * {@link #reset} if that attempt succeeds.
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

    /**
     * Checks the lock and counts the attempt atomically per address ({@link Map#compute}), so
     * no more than {@value #MAX_FAILED_ATTEMPTS} attempts get through, however many arrive at once.
     */
    @Override
    public void countAttempt(EmailAddress email) {
        Instant now = clock.instant();
        attempts.values().removeIf(entry -> entry.isExpiredAt(now));
        AtomicBoolean locked = new AtomicBoolean();
        attempts.compute(email, (_, current) -> {
            if (current != null && current.isLockedAt(now)) {
                locked.set(true);
                return current;
            }
            return current == null ? Attempts.first(now) : current.afterFailure(now);
        });
        if (locked.get()) {
            throw new LoginLockedException();
        }
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
