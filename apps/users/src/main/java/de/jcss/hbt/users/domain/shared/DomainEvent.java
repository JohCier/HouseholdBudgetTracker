package de.jcss.hbt.users.domain.shared;

import java.time.Instant;

/**
 * Something that happened in the domain, recorded by an {@link AggregateRoot} and
 * published by the application layer after the aggregate has been saved. Implementations
 * are immutable records carrying only the minimal information needed to identify what
 * changed (usually just ids) and when it happened; listeners load anything else they
 * need themselves.
 */
public interface DomainEvent {

    /** When the change was made in the aggregate, not when the event was published. */
    Instant occurredAt();
}
