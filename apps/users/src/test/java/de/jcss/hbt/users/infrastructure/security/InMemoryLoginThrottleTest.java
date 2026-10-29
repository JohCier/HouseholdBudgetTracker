package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.application.user.LoginLockedException;
import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
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
            throttle.countAttempt(EMAIL);
        }
    }

    /** Whether another attempt gets through; that attempt is counted, too. */
    private void assertNotLocked(EmailAddress email) {
        assertThatCode(() -> throttle.countAttempt(email)).doesNotThrowAnyException();
    }

    private void assertLocked(EmailAddress email) {
        assertThatThrownBy(() -> throttle.countAttempt(email)).isInstanceOf(LoginLockedException.class);
    }

    @Test
    void fourFailedAttemptsDoNotLock() {
        failedAttempts(4, Duration.ofMinutes(1));

        assertNotLocked(EMAIL);
    }

    @Test
    void theFifthFailedAttemptWithinFifteenMinutesLocksForFifteenMinutes() {
        // The fifth attempt at 14:00 after the first, still within the window
        failedAttempts(5, Duration.ofSeconds(210));
        Instant fifth = now;

        now = fifth.plus(Duration.ofMinutes(14));
        assertLocked(EMAIL);
        now = fifth.plus(Duration.ofMinutes(15));
        assertNotLocked(EMAIL);
    }

    @Test
    void attemptsOutsideTheWindowStartANewCount() {
        // Five attempts, but 4 minutes apart: the fifth is 16 minutes after the first
        failedAttempts(5, Duration.ofMinutes(4));

        assertNotLocked(EMAIL);
    }

    @Test
    void afterALockFiveNewFailedAttemptsAreNeededToLockAgain() {
        failedAttempts(5, Duration.ofSeconds(1));
        now = now.plus(InMemoryLoginThrottle.LOCK_DURATION);

        failedAttempts(4, Duration.ofSeconds(1));

        assertNotLocked(EMAIL);
    }

    @Test
    void resetForgetsTheFailedAttempts() {
        failedAttempts(4, Duration.ofSeconds(1));

        throttle.reset(EMAIL);
        throttle.countAttempt(EMAIL);

        assertNotLocked(EMAIL);
    }

    @Test
    void addressesAreCountedSeparately() {
        failedAttempts(5, Duration.ofSeconds(1));

        assertNotLocked(EmailAddress.of("moritz@example.org"));
    }

    @Test
    void theFifthAttemptIsStillCheckedAndASuccessLiftsTheLock() {
        failedAttempts(4, Duration.ofSeconds(1));

        assertNotLocked(EMAIL);
        throttle.reset(EMAIL);

        assertNotLocked(EMAIL);
    }

    @Test
    void aLockedAttemptIsNotCounted() {
        failedAttempts(5, Duration.ofSeconds(1));
        Instant fifth = now;
        now = fifth.plus(Duration.ofMinutes(10));
        assertLocked(EMAIL);

        now = fifth.plus(InMemoryLoginThrottle.LOCK_DURATION);
        failedAttempts(4, Duration.ofSeconds(1));

        assertNotLocked(EMAIL);
    }

    @Test
    void parallelAttemptsCannotGetPastTheLimit() throws InterruptedException {
        int parallelAttempts = 50;
        AtomicInteger passed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(parallelAttempts)) {
            for (int i = 0; i < parallelAttempts; i++) {
                executor.submit(() -> {
                    start.await();
                    try {
                        throttle.countAttempt(EMAIL);
                        passed.incrementAndGet();
                    } catch (LoginLockedException e) {
                        // expected for all but the first attempts
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(passed).hasValue(InMemoryLoginThrottle.MAX_FAILED_ATTEMPTS);
    }
}
