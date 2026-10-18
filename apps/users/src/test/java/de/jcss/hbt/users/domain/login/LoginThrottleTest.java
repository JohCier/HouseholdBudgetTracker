package de.jcss.hbt.users.domain.login;

import de.jcss.hbt.users.domain.exception.LoginLockedException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AN-10 */
class LoginThrottleTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");
    private static final Instant START = Instant.parse("2026-10-18T12:00:00Z");

    private static LoginThrottle failedAttempts(int count, Duration apart) {
        LoginThrottle throttle = LoginThrottle.start(EMAIL);
        for (int i = 0; i < count; i++) {
            throttle = throttle.afterFailedAttempt(START.plus(apart.multipliedBy(i)));
        }
        return throttle;
    }

    @Test
    void fourFailedAttemptsDoNotLock() {
        LoginThrottle throttle = failedAttempts(4, Duration.ofMinutes(1));

        assertThat(throttle.failedAttempts()).isEqualTo(4);
        assertThatCode(() -> throttle.ensureNotLocked(START.plus(Duration.ofMinutes(4)))).doesNotThrowAnyException();
    }

    @Test
    void theFifthFailedAttemptWithinFifteenMinutesLocksForFifteenMinutes() {
        // The fifth attempt at 14:00 after the first, still within the window
        LoginThrottle throttle = failedAttempts(5, Duration.ofSeconds(210));
        Instant fifth = START.plus(Duration.ofSeconds(840));

        assertThat(throttle.lockedUntil()).isEqualTo(fifth.plus(Duration.ofMinutes(15)));
        assertThatThrownBy(() -> throttle.ensureNotLocked(fifth.plus(Duration.ofMinutes(14))))
                .isInstanceOf(LoginLockedException.class);
        assertThatCode(() -> throttle.ensureNotLocked(fifth.plus(Duration.ofMinutes(15))))
                .doesNotThrowAnyException();
    }

    @Test
    void attemptsOutsideTheWindowStartANewCount() {
        // Five attempts, but 4 minutes apart: the fifth is 16 minutes after the first
        LoginThrottle throttle = failedAttempts(5, Duration.ofMinutes(4));

        assertThat(throttle.lockedUntil()).isNull();
        assertThat(throttle.failedAttempts()).isEqualTo(1);
        assertThat(throttle.windowStartedAt()).isEqualTo(START.plus(Duration.ofMinutes(16)));
    }

    @Test
    void afterALockFiveNewFailedAttemptsAreNeededToLockAgain() {
        LoginThrottle locked = failedAttempts(5, Duration.ofSeconds(1));
        Instant afterLock = locked.lockedUntil();

        LoginThrottle again = locked.afterFailedAttempt(afterLock);

        assertThat(again.failedAttempts()).isEqualTo(1);
        assertThatCode(() -> again.ensureNotLocked(afterLock)).doesNotThrowAnyException();
    }

    @Test
    void equalityIsBasedOnTheAddress() {
        assertThat(failedAttempts(3, Duration.ofSeconds(1))).isEqualTo(LoginThrottle.start(EMAIL));
    }

    @Test
    void rejectsInconsistentState() {
        assertThatThrownBy(() -> LoginThrottle.reconstitute(EMAIL, 5, START, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LoginThrottle.reconstitute(EMAIL, 2, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
