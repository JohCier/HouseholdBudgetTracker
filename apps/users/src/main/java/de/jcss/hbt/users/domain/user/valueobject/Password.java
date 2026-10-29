package de.jcss.hbt.users.domain.user.valueobject;

import de.jcss.hbt.users.domain.exception.DomainValidationException;
import de.jcss.hbt.users.domain.user.User;
import org.jmolecules.ddd.annotation.ValueObject;

import java.nio.charset.StandardCharsets;

/**
 * A password in plain text as entered by the user, e.g. when registering or signing in.
 * Never stored: a {@link User} only keeps its {@link PasswordHash}. Its {@link #toString()}
 * does not reveal it, so it cannot end up in logs by accident.
 */
@ValueObject
public record Password(String value) {

    public static final int MIN_LENGTH = 8;
    /** Longer passwords would be cut off silently by BCrypt. */
    public static final int MAX_BYTES = 72;

    public Password {
        if (value == null) {
            throw new DomainValidationException("password must not be null");
        }
        if (value.codePointCount(0, value.length()) < MIN_LENGTH) {
            throw new DomainValidationException("password must have at least %d characters".formatted(MIN_LENGTH));
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new DomainValidationException("password must not be longer than %d bytes".formatted(MAX_BYTES));
        }
    }

    public static Password of(String value) {
        return new Password(value);
    }

    @Override
    public String toString() {
        return "Password[***]";
    }
}
