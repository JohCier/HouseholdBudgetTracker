package de.jcss.hbt.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity living inside an aggregate: it has an identity of its own (equality is
 * based on that identity only), but it is created, changed and removed exclusively
 * through its {@link AggregateRoot}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DomainEntity {
}
