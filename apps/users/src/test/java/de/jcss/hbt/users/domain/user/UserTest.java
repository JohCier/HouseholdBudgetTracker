package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.user.event.UserRegistered;
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
