package de.jcss.hbt.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the root entity of an aggregate: the only object of the aggregate that may be
 * referenced from outside, loaded and saved through a repository, and the guardian of
 * the aggregate's invariants. Every change to the aggregate goes through its root, which
 * records what happened as {@link DomainEvent}s.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AggregateRoot {
}
