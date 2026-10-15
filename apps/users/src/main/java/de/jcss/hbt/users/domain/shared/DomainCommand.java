package de.jcss.hbt.users.domain.shared;

/**
 * An intention to change the state of an {@link AggregateRoot}, named after the business
 * action in the imperative (e.g. "add expense"); each command is handled in exactly one
 * transaction. Commands are part of the domain: implementations are immutable records,
 * preferably made of value objects, that only check their input formally (e.g. for
 * {@code null}). Whether the change is allowed is decided by the aggregate, not by the
 * command.
 */
public interface DomainCommand {
}
