package de.jcss.hbt.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the interface through which a {@link ReadModel} is loaded. Query handlers are
 * declared in the domain next to their read model and implemented in the persistence
 * infrastructure, reading directly from the store without going through the aggregates
 * or their repositories. They never change state.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QueryHandler {
}
