package de.jcss.hbt.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an immutable, query-optimized view of the data, shaped for what a client wants to
 * display. Read models are loaded by a {@link QueryHandler} straight from the store,
 * never by reconstituting aggregates, and they cannot be used to change anything.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReadModel {
}
