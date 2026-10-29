package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.PasswordUnchangedException;
import de.jcss.hbt.users.domain.exception.WrongCurrentPasswordException;
import de.jcss.hbt.users.domain.user.event.PasswordChanged;
import de.jcss.hbt.users.domain.user.event.UserRegistered;
import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import de.jcss.hbt.users.domain.user.valueobject.Password;
import de.jcss.hbt.users.domain.user.valueobject.PasswordHash;
import de.jcss.hbt.users.domain.user.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");
    private static final Password PASSWORD = Password.of("richtig-geheim");

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final UserId id = UserId.generate();

    @Test
    void registerKeepsOnlyTheHashOfThePasswordAndRecordsIt() {
        User user = User.register(id, EMAIL, PASSWORD, hasher);

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.email()).isEqualTo(EMAIL);
        assertThat(user.passwordHash()).isEqualTo(hasher.hash(PASSWORD));
        assertThat(user.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new UserRegistered(id, Instant.now()));
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void authenticatesWithTheRightPasswordOnly() {
        User user = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));

        assertThatCode(() -> user.authenticate(PASSWORD, hasher)).doesNotThrowAnyException();
        assertThatThrownBy(() -> user.authenticate(Password.of("falsch-geheim"), hasher))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /** PA-1, PA-3 */
    @Test
    void changePasswordKeepsOnlyTheHashOfTheNewPasswordAndRecordsIt() {
        User user = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));
        Password newPassword = Password.of("neues-geheim");

        User changed = user.changePassword(PASSWORD, newPassword, hasher);

        assertThat(changed).isEqualTo(user);
        assertThat(changed.email()).isEqualTo(EMAIL);
        assertThat(changed.passwordHash()).isEqualTo(hasher.hash(newPassword));
        assertThatCode(() -> changed.authenticate(newPassword, hasher)).doesNotThrowAnyException();
        assertThatThrownBy(() -> changed.authenticate(PASSWORD, hasher))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(changed.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new PasswordChanged(id, Instant.now()));
    }

    /** PA-2 */
    @Test
    void changePasswordNeedsTheRightCurrentPassword() {
        User user = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));

        assertThatThrownBy(() -> user.changePassword(Password.of("falsch-geheim"), Password.of("neues-geheim"), hasher))
                .isInstanceOf(WrongCurrentPasswordException.class);
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    /** PA-4 */
    @Test
    void theNewPasswordMustDifferFromTheCurrentOne() {
        User user = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));

        assertThatThrownBy(() -> user.changePassword(PASSWORD, PASSWORD, hasher))
                .isInstanceOf(PasswordUnchangedException.class);
    }

    @Test
    void reconstituteRecordsNoEvents() {
        assertThat(User.reconstitute(id, EMAIL, hasher.hash(PASSWORD)).pullDomainEvents()).isEmpty();
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        User first = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));
        User second = User.reconstitute(id, EmailAddress.of("other@example.org"), PasswordHash.of("x"));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(User.reconstitute(UserId.generate(), EMAIL, hasher.hash(PASSWORD)));
    }

    @Test
    void toStringDoesNotRevealThePasswordHash() {
        User user = User.reconstitute(id, EMAIL, hasher.hash(PASSWORD));

        assertThat(user.toString()).doesNotContain("hashed").contains(EMAIL.value());
    }
}
