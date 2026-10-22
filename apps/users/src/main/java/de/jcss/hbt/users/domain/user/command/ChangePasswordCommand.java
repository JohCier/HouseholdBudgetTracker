package de.jcss.hbt.users.domain.user.command;

import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.UserId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Change the password of the signed-in user, who proves it is them with their current password (PA-1). */
@Command
public record ChangePasswordCommand(UserId userId, Password currentPassword, Password newPassword) {

    public ChangePasswordCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(currentPassword, "currentPassword must not be null");
        Objects.requireNonNull(newPassword, "newPassword must not be null");
    }
}
