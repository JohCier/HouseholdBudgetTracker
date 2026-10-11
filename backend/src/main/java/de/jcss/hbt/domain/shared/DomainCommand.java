package de.jcss.hbt.domain.shared;

/**
 * An intention to change the state of an {@link AggregateRoot}, named in the imperative
 * (e.g. "add expense"). Implementations are immutable records carrying the raw input;
 * whether the change is allowed is decided by the aggregate, not by the command.
 */
public interface DomainCommand {
}
