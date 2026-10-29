package de.jcss.hbt.users.domain.user.command;

import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import de.jcss.hbt.users.domain.user.valueobject.Password;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Register a new user who signs in with the given e-mail address and password. */
@Command
public record RegisterUserCommand(EmailAddress email, Password password) {

    public RegisterUserCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }
}
