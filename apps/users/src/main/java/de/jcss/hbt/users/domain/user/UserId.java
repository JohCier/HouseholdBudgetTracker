package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.shared.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link User} aggregate. Other apps refer to users by this id, e.g. as the
 * owner of a household plan.
 */
@ValueObject
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
