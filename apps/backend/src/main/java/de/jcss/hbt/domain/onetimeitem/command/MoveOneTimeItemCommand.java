package de.jcss.hbt.domain.onetimeitem.command;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Move a one-time item of the given owner between two neighbors in its area (ES-4).
 *
 * @param previousItemId the item that is to come right before, {@code null} to move it to the start
 * @param nextItemId     the item that is to come right after, {@code null} to move it to the end
 */
@Command
public record MoveOneTimeItemCommand(OwnerId owner, OneTimeItemId itemId, OneTimeItemId previousItemId,
                                     OneTimeItemId nextItemId) {

    public MoveOneTimeItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
