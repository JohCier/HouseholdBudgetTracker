package de.jcss.hbt.users.domain.user.event;

import de.jcss.hbt.users.domain.shared.DomainEvent;
import de.jcss.hbt.users.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/** A new user registered. */
public record UserRegistered(UserId userId, Instant occurredAt) implements DomainEvent {

    public UserRegistered {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
