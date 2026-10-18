package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.LoginLockedException;
import de.jcss.hbt.users.domain.login.LoginThrottle;
import de.jcss.hbt.users.domain.login.LoginThrottleRepository;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.FakePasswordHasher;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.User;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.UserRepository;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import de.jcss.hbt.users.domain.user.event.UserRegistered;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link UserApplicationService} registers and authenticates users correctly,
 * using an in-memory fake repository and capturing the published events.
 */
class UserApplicationServiceTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");
    private static final Password PASSWORD = Password.of("richtig-geheim");

    private final FakeRepository repository = new FakeRepository();
    private final FakeThrottleRepository throttles = new FakeThrottleRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private Instant now = Instant.parse("2026-10-18T12:00:00Z");
    private final Clock clock = new Clock() {
        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    };
    private final UserApplicationService service =
            new UserApplicationService(repository, throttles, new FakePasswordHasher(), publishedEvents::add, clock);

    @Test
    void registerStoresTheUserAndPublishesIt() {
        UserId id = service.register(new RegisterUserCommand(EMAIL, PASSWORD));

        assertThat(repository.users.get(EMAIL).id()).isEqualTo(id);
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(UserRegistered.class, event -> assertThat(event.userId()).isEqualTo(id));
    }

    @Test
    void anAddressCanOnlyBeRegisteredOnce() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        publishedEvents.clear();

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand(EmailAddress.of("MAX@example.org"), Password.of("anderes-geheim"))))
                .isInstanceOf(EmailAddressAlreadyRegisteredException.class);
        assertThat(repository.users).hasSize(1);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void authenticateTellsWhoSignsIn() {
        UserId id = service.register(new RegisterUserCommand(EMAIL, PASSWORD));

        assertThat(service.authenticate(EmailAddress.of("Max@Example.org"), PASSWORD)).isEqualTo(id);
    }

    @Test
    void wrongPasswordAndUnknownAddressFailAlike() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));

        assertThatThrownBy(() -> service.authenticate(EMAIL, Password.of("falsch-geheim")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail address or password is wrong");
        assertThatThrownBy(() -> service.authenticate(EmailAddress.of("unknown@example.org"), PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail address or password is wrong");
    }

    private void failToSignIn(EmailAddress email, int times) {
        for (int i = 0; i < times; i++) {
            assertThatThrownBy(() -> service.authenticate(email, Password.of("falsch-geheim")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    /** AN-10 */
    @Test
    void fiveFailedAttemptsLockTheAddressEvenForTheRightPassword() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));

        failToSignIn(EMAIL, 5);

        assertThatThrownBy(() -> service.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(LoginLockedException.class);
        now = now.plus(Duration.ofMinutes(15));
        assertThat(service.authenticate(EMAIL, PASSWORD)).isNotNull();
    }

    /** AN-10, AN-6 */
    @Test
    void unknownAddressesAreLockedAlike() {
        EmailAddress unknown = EmailAddress.of("unknown@example.org");

        failToSignIn(unknown, 5);

        assertThatThrownBy(() -> service.authenticate(unknown, PASSWORD))
                .isInstanceOf(LoginLockedException.class);
    }

    /** AN-10 */
    @Test
    void aSuccessfulSignInForgetsTheFailedAttempts() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        failToSignIn(EMAIL, 4);

        service.authenticate(EMAIL, PASSWORD);

        assertThat(throttles.throttles).doesNotContainKey(EMAIL);
        failToSignIn(EMAIL, 4);
        assertThat(service.authenticate(EMAIL, PASSWORD)).isNotNull();
    }

    private static final class FakeThrottleRepository implements LoginThrottleRepository {

        private final Map<EmailAddress, LoginThrottle> throttles = new HashMap<>();

        @Override
        public Optional<LoginThrottle> findByEmail(EmailAddress email) {
            return Optional.ofNullable(throttles.get(email));
        }

        @Override
        public void save(LoginThrottle throttle) {
            throttles.put(throttle.email(), throttle);
        }

        @Override
        public void remove(EmailAddress email) {
            throttles.remove(email);
        }
    }

    private static final class FakeRepository implements UserRepository {

        private final Map<EmailAddress, User> users = new HashMap<>();

        @Override
        public Optional<User> findByEmail(EmailAddress email) {
            return Optional.ofNullable(users.get(email))
                    .map(user -> User.reconstitute(user.id(), user.email(), user.passwordHash()));
        }

        @Override
        public boolean existsByEmail(EmailAddress email) {
            return users.containsKey(email);
        }

        @Override
        public void add(User user) {
            if (users.putIfAbsent(user.email(), user) != null) {
                throw new EmailAddressAlreadyRegisteredException(user.email());
            }
        }
    }
}
