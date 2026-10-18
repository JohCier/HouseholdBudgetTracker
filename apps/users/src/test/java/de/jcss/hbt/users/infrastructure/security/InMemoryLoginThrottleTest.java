package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.application.user.LoginLockedException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AN-10 */
class InMemoryLoginThrottleTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");

    private Instant now = Instant.parse("2026-10-18T12:00:00Z");
    private final InMemoryLoginThrottle throttle = new InMemoryLoginThrottle(new Clock() {
        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    });

    private void failedAttempts(int count, Duration apart) {
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                now = now.plus(apart);
            }
            throttle.recordFailure(EMAIL);
        }
    }

    @Test
    void fourFailedAttemptsDoNotLock() {
        failedAttempts(4, Duration.ofMinutes(1));

        assertThatCode(() -> throttle.ensureNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void theFifthFailedAttemptWithinFifteenMinutesLocksForFifteenMinutes() {
        // The fifth attempt at 14:00 after the first, still within the window
        failedAttempts(5, Duration.ofSeconds(210));
        Instant fifth = now;

        now = fifth.plus(Duration.ofMinutes(14));
        assertThatThrownBy(() -> throttle.ensureNotLocked(EMAIL)).isInstanceOf(LoginLockedException.class);
        now = fifth.plus(Duration.ofMinutes(15));
        assertThatCode(() -> throttle.ensureNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void attemptsOutsideTheWindowStartANewCount() {
        // Five attempts, but 4 minutes apart: the fifth is 16 minutes after the first
        failedAttempts(5, Duration.ofMinutes(4));

        assertThatCode(() -> throttle.ensureNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void afterALockFiveNewFailedAttemptsAreNeededToLockAgain() {
        failedAttempts(5, Duration.ofSeconds(1));
        now = now.plus(InMemoryLoginThrottle.LOCK_DURATION);

        failedAttempts(4, Duration.ofSeconds(1));

        assertThatCode(() -> throttle.ensureNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void resetForgetsTheFailedAttempts() {
        failedAttempts(4, Duration.ofSeconds(1));

        throttle.reset(EMAIL);
        throttle.recordFailure(EMAIL);

        assertThatCode(() -> throttle.ensureNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void addressesAreCountedSeparately() {
        failedAttempts(5, Duration.ofSeconds(1));

        assertThatCode(() -> throttle.ensureNotLocked(EmailAddress.of("moritz@example.org")))
                .doesNotThrowAnyException();
    }
}
