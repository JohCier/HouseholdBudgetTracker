package de.jcss.hbt.users.application.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the entry point of the write side: it handles {@code DomainCommand}s by loading an
 * aggregate, invoking its behaviour, saving it and publishing the recorded
 * {@code DomainEvent}s, all within one transaction. Contains no business rules itself.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApplicationService {
}
