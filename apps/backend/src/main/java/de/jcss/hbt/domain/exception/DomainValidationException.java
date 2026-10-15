package de.jcss.hbt.domain.exception;

/**
 * Base class for violations of a domain invariant caused by invalid input, e.g. a
 * non-positive amount. Adapters can treat all subclasses uniformly as client errors.
 */
public abstract class DomainValidationException extends RuntimeException {

    protected DomainValidationException(String message) {
        super(message);
    }
}
