package de.jcss.hbt.users.domain.user.event;

import de.jcss.hbt.users.domain.user.UserId;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/** A new user registered. */
public record UserRegistered(UserId userId, Instant timestamp) implements DomainEvent {

    public UserRegistered {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
