package de.jcss.hbt.domain.exception;

/**
 * Base of all exceptions for something that would be created although it already exists. Adapters translate every
 * subclass the same way (e.g. HTTP 409); the {@link #code() code} tells clients what already exists.
 */
public abstract class AlreadyExistsException extends RuntimeException {

    private final String code;

    protected AlreadyExistsException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stable, kebab-case error code, e.g. {@code plan-already-exists}; clients map it to their own message. */
    public String code() {
        return code;
    }
}
