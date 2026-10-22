package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.WrongCurrentPasswordException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.FakePasswordHasher;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.PasswordHash;
import de.jcss.hbt.users.domain.user.PasswordHasher;
import de.jcss.hbt.users.domain.user.User;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.UserRepository;
import de.jcss.hbt.users.domain.user.command.ChangePasswordCommand;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import de.jcss.hbt.users.domain.user.event.PasswordChanged;
import de.jcss.hbt.users.domain.user.event.UserRegistered;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link UserApplicationService} registers and authenticates users and changes
 * their passwords correctly,
 * using in-memory fakes and capturing the published events.
 */
class UserApplicationServiceTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");
    private static final Password PASSWORD = Password.of("richtig-geheim");

    private final FakeRepository repository = new FakeRepository();
    private final FakeLoginThrottle loginThrottle = new FakeLoginThrottle();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final UserApplicationService service =
            new UserApplicationService(repository, loginThrottle, new FakePasswordHasher(), publishedEvents::add);

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

    /** AN-10, AN-6 */
    @Test
    void everySignInIsCountedBeforeThePasswordIsChecked() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        EmailAddress unknown = EmailAddress.of("unknown@example.org");

        assertThatThrownBy(() -> service.authenticate(EMAIL, Password.of("falsch-geheim")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.authenticate(unknown, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(loginThrottle.attempts).containsExactly(EMAIL, unknown);
        assertThat(loginThrottle.resets).isEmpty();
    }

    /** AN-6 */
    @Test
    void anUnknownAddressStillHasItsPasswordChecked() {
        List<Password> checked = new ArrayList<>();
        FakePasswordHasher fake = new FakePasswordHasher();
        PasswordHasher hasher = new PasswordHasher() {
            @Override
            public PasswordHash hash(Password password) {
                return fake.hash(password);
            }

            @Override
            public boolean matches(Password password, PasswordHash hash) {
                checked.add(password);
                return fake.matches(password, hash);
            }
        };
        UserApplicationService service =
                new UserApplicationService(repository, loginThrottle, hasher, publishedEvents::add);

        assertThatThrownBy(() -> service.authenticate(EmailAddress.of("unknown@example.org"), PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(checked).containsExactly(PASSWORD);
    }

    /** AN-10 */
    @Test
    void aLockedAddressCannotSignInEvenWithTheRightPassword() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        loginThrottle.locked.add(EMAIL);

        assertThatThrownBy(() -> service.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(LoginLockedException.class);
        assertThat(loginThrottle.attempts).isEmpty();
    }

    /** AN-10 */
    @Test
    void aSuccessfulSignInResetsTheThrottle() {
        service.register(new RegisterUserCommand(EMAIL, PASSWORD));

        service.authenticate(EMAIL, PASSWORD);

        assertThat(loginThrottle.attempts).containsExactly(EMAIL);
        assertThat(loginThrottle.resets).containsExactly(EMAIL);
    }

    /** PA-1, PA-6 */
    @Test
    void changePasswordStoresTheNewPasswordAndPublishesIt() {
        UserId id = service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        publishedEvents.clear();
        Password newPassword = Password.of("neues-geheim");

        service.changePassword(new ChangePasswordCommand(id, PASSWORD, newPassword));

        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(PasswordChanged.class, event -> assertThat(event.userId()).isEqualTo(id));
        assertThat(loginThrottle.attempts).containsExactly(EMAIL);
        assertThat(loginThrottle.resets).containsExactly(EMAIL);
        assertThat(service.authenticate(EMAIL, newPassword)).isEqualTo(id);
        assertThatThrownBy(() -> service.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /** PA-2, PA-6 */
    @Test
    void aWrongCurrentPasswordCountsAsFailedSignIn() {
        UserId id = service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        publishedEvents.clear();

        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordCommand(id, Password.of("falsch-geheim"), Password.of("neues-geheim"))))
                .isInstanceOf(WrongCurrentPasswordException.class);

        assertThat(loginThrottle.attempts).containsExactly(EMAIL);
        assertThat(loginThrottle.resets).isEmpty();
        assertThat(publishedEvents).isEmpty();
        assertThat(service.authenticate(EMAIL, PASSWORD)).isEqualTo(id);
    }

    /** PA-6 */
    @Test
    void aLockedAddressCannotChangeItsPassword() {
        UserId id = service.register(new RegisterUserCommand(EMAIL, PASSWORD));
        loginThrottle.locked.add(EMAIL);

        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordCommand(id, PASSWORD, Password.of("neues-geheim"))))
                .isInstanceOf(LoginLockedException.class);
        assertThat(repository.users.get(EMAIL).passwordHash()).isEqualTo(new FakePasswordHasher().hash(PASSWORD));
    }

    private static final class FakeLoginThrottle implements LoginThrottle {

        private final Set<EmailAddress> locked = new HashSet<>();
        private final List<EmailAddress> attempts = new ArrayList<>();
        private final List<EmailAddress> resets = new ArrayList<>();

        @Override
        public void countAttempt(EmailAddress email) {
            if (locked.contains(email)) {
                throw new LoginLockedException();
            }
            attempts.add(email);
        }

        @Override
        public void reset(EmailAddress email) {
            resets.add(email);
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
        public Optional<User> findById(UserId id) {
            return users.values().stream().filter(user -> user.id().equals(id)).findFirst()
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

        @Override
        public void save(User user) {
            users.put(user.email(), user);
        }
    }
}
