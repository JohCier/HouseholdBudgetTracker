package de.jcss.hbt.users.domain.user.command;

import de.jcss.hbt.users.domain.shared.DomainCommand;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.Password;

import java.util.Objects;

/** Register a new user who signs in with the given e-mail address and password. */
public record RegisterUserCommand(EmailAddress email, Password password) implements DomainCommand {

    public RegisterUserCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }
}
