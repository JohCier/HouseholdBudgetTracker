package de.jcss.hbt.domain.shared;

import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

/**
 * Common type of all domain events of this app: each one records when the change it
 * describes was made in the domain. Listeners that are interested in every event, such
 * as an audit log, can subscribe to this type instead of to each event on its own.
 */
public interface TimestampedDomainEvent extends DomainEvent {

    /** When the change happened in the domain. */
    Instant timestamp();
}
