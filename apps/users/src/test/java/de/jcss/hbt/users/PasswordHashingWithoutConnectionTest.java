package de.jcss.hbt.users;

import de.jcss.hbt.users.application.user.UserApplicationService;
import de.jcss.hbt.users.domain.user.PasswordHasher;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import de.jcss.hbt.users.domain.user.valueobject.Password;
import de.jcss.hbt.users.domain.user.valueobject.PasswordHash;
import de.jcss.hbt.users.infrastructure.security.BCryptPasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hashing and checking passwords is deliberately slow (BCrypt), so it must not hold one of the
 * few pooled database connections. A connection is only kept across statements while
 * transaction synchronization is active; it must not be while a password is hashed or checked.
 */
@IntegrationTest
class PasswordHashingWithoutConnectionTest {

    private static final EmailAddress EMAIL = EmailAddress.of("max@example.org");
    private static final Password PASSWORD = Password.of("richtig-geheim");

    @Autowired
    private UserApplicationService users;

    @Autowired
    private RecordingPasswordHasher hasher;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM users.app_user").update();
        hasher.synchronizationActive.clear();
    }

    @Test
    void registeringAndSigningInHashOutsideOfATransaction() {
        users.register(new RegisterUserCommand(EMAIL, PASSWORD));
        users.authenticate(EMAIL, PASSWORD);

        assertThat(hasher.synchronizationActive).containsExactly(false, false);
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        RecordingPasswordHasher recordingPasswordHasher(BCryptPasswordHasher bcrypt) {
            return new RecordingPasswordHasher(bcrypt);
        }
    }

    static final class RecordingPasswordHasher implements PasswordHasher {

        private final PasswordHasher delegate;
        private final List<Boolean> synchronizationActive = new ArrayList<>();

        RecordingPasswordHasher(PasswordHasher delegate) {
            this.delegate = delegate;
        }

        @Override
        public PasswordHash hash(Password password) {
            synchronizationActive.add(TransactionSynchronizationManager.isSynchronizationActive());
            return delegate.hash(password);
        }

        @Override
        public boolean matches(Password password, PasswordHash hash) {
            synchronizationActive.add(TransactionSynchronizationManager.isSynchronizationActive());
            return delegate.matches(password, hash);
        }
    }
}
