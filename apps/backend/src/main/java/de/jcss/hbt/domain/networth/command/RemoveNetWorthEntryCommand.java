package de.jcss.hbt.domain.networth.command;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/** Remove an asset or a liability the owner recorded for a year (VS-5, VS-7). */
@Command
public record RemoveNetWorthEntryCommand(OwnerId owner, Year year, NetWorthEntryId entryId) {

    public RemoveNetWorthEntryCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
    }
}
