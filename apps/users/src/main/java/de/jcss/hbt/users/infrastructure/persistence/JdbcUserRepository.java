package de.jcss.hbt.users.infrastructure.persistence;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.PasswordHash;
import de.jcss.hbt.users.domain.user.User;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link User} aggregates in the {@code users.app_user} table. The schema
 * {@code users} belongs to this app alone; the household plans of the backend live in
 * the same database, but only refer to users by their id.
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> User.reconstitute(
            UserId.of(rs.getObject("id", UUID.class)),
            EmailAddress.of(rs.getString("email")),
            PasswordHash.of(rs.getString("password_hash")));

    private final JdbcClient jdbcClient;

    public JdbcUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return jdbcClient.sql("SELECT id, email, password_hash FROM users.app_user WHERE email = :email")
                .param("email", email.value())
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jdbcClient.sql("SELECT id, email, password_hash FROM users.app_user WHERE id = :id")
                .param("id", id.value())
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM users.app_user WHERE email = :email)")
                .param("email", email.value())
                .query(Boolean.class)
                .single();
    }

    @Override
    public void add(User user) {
        try {
            jdbcClient.sql("""
                            INSERT INTO users.app_user (id, email, password_hash)
                            VALUES (:id, :email, :passwordHash)
                            """)
                    .param("id", user.id().value())
                    .param("email", user.email().value())
                    .param("passwordHash", user.passwordHash().value())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new EmailAddressAlreadyRegisteredException(user.email());
        }
    }

    @Override
    public void save(User user) {
        jdbcClient.sql("UPDATE users.app_user SET email = :email, password_hash = :passwordHash WHERE id = :id")
                .param("id", user.id().value())
                .param("email", user.email().value())
                .param("passwordHash", user.passwordHash().value())
                .update();
    }
}
