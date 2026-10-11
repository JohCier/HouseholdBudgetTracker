package de.jcss.hbt.domain.shared;

/**
 * Something that happened in the domain, recorded by an {@link AggregateRoot} and
 * published by the application layer after the aggregate has been saved. Implementations
 * are immutable records carrying only the minimal information needed to identify what
 * changed (usually just ids); listeners load anything else they need themselves.
 */
public interface DomainEvent {
}
