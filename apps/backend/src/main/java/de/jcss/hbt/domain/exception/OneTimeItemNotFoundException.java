package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;

/**
 * Thrown when a {@code OneTimeItem} is referenced by an id that does not exist, or belongs to someone else (EP-4).
 */
public class OneTimeItemNotFoundException extends RuntimeException {

    public OneTimeItemNotFoundException(OneTimeItemId id) {
        super("One-time item not found: " + id.value());
    }
}
