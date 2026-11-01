package de.jcss.hbt.domain.exception;

/**
 * Base of all exceptions for something referenced that does not exist, or belongs to someone else. Adapters translate
 * every subclass the same way (e.g. HTTP 404); the {@link #code() code} tells clients which thing was not found.
 */
public abstract class NotFoundException extends RuntimeException {

    private final String code;

    protected NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stable, kebab-case error code, e.g. {@code plan-not-found}; clients map it to their own message. */
    public String code() {
        return code;
    }
}
