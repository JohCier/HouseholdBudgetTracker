package de.jcss.hbt.users.infrastructure.persistence;

import de.jcss.hbt.users.domain.login.LoginThrottle;
import de.jcss.hbt.users.domain.login.LoginThrottleRepository;
import de.jcss.hbt.users.domain.user.EmailAddress;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Stores {@link LoginThrottle} aggregates in the {@code users.login_throttle} table. Expects
 * to run within the transaction of the calling application service.
 */
@Repository
public class JdbcLoginThrottleRepository implements LoginThrottleRepository {

    private static final RowMapper<LoginThrottle> ROW_MAPPER = (rs, rowNum) -> LoginThrottle.reconstitute(
            EmailAddress.of(rs.getString("email")),
            rs.getInt("failed_attempts"),
            toInstant(rs.getTimestamp("window_started_at")),
            toInstant(rs.getTimestamp("locked_until")));

    private final JdbcClient jdbcClient;

    public JdbcLoginThrottleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    @Override
    public Optional<LoginThrottle> findByEmail(EmailAddress email) {
        return jdbcClient.sql("""
                        SELECT email, failed_attempts, window_started_at, locked_until
                        FROM users.login_throttle
                        WHERE email = :email
                        FOR UPDATE
                        """)
                .param("email", email.value())
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public void save(LoginThrottle throttle) {
        jdbcClient.sql("""
                        INSERT INTO users.login_throttle (email, failed_attempts, window_started_at, locked_until)
                        VALUES (:email, :failedAttempts, :windowStartedAt, :lockedUntil)
                        ON CONFLICT (email) DO UPDATE
                        SET failed_attempts = EXCLUDED.failed_attempts,
                            window_started_at = EXCLUDED.window_started_at,
                            locked_until = EXCLUDED.locked_until
                        """)
                .param("email", throttle.email().value())
                .param("failedAttempts", throttle.failedAttempts())
                .param("windowStartedAt", toTimestamp(throttle.windowStartedAt()))
                .param("lockedUntil", toTimestamp(throttle.lockedUntil()))
                .update();
    }

    @Override
    public void remove(EmailAddress email) {
        jdbcClient.sql("DELETE FROM users.login_throttle WHERE email = :email")
                .param("email", email.value())
                .update();
    }
}
