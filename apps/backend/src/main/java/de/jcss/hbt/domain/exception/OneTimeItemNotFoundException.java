package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;

/**
 * Thrown when a {@code OneTimeItem} is referenced by an id that does not exist, or belongs to someone else (EP-4).
 */
public class OneTimeItemNotFoundException extends NotFoundException {

    private static final String CODE = "one-time-item-not-found";

    public OneTimeItemNotFoundException(OneTimeItemId id) {
        super(CODE, "One-time item not found: " + id.value());
    }
}
