package de.jcss.hbt.users.domain.user.valueobject;

import de.jcss.hbt.users.domain.user.PasswordHasher;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * One-way hash of a {@link Password}, as created by a {@link PasswordHasher}; the only form
 * in which a password is kept. Its {@link #toString()} does not reveal it.
 */
@ValueObject
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}
