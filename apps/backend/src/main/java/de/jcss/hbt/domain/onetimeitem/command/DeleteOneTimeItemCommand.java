package de.jcss.hbt.domain.onetimeitem.command;

import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Delete a one-time item of the given owner for good (EP-7, EP-17). */
@Command
public record DeleteOneTimeItemCommand(OwnerId owner, OneTimeItemId itemId) {

    public DeleteOneTimeItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
