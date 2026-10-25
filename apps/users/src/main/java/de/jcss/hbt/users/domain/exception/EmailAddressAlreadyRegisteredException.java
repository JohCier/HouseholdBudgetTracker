package de.jcss.hbt.users.domain.exception;

import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;

/** Thrown when a user would be registered with an e-mail address that already has an account. */
public class EmailAddressAlreadyRegisteredException extends RuntimeException {

    public EmailAddressAlreadyRegisteredException(EmailAddress email) {
        super("%s is already registered".formatted(email.value()));
    }
}
