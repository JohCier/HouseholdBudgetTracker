package de.jcss.hbt.domain.networth.command;

import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthEntryName;
import de.jcss.hbt.domain.networth.NetWorthEntryType;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/** Change the type, name and amount of an asset or a liability the owner recorded for a year (VS-5). */
@Command
public record ChangeNetWorthEntryCommand(OwnerId owner, Year year, NetWorthEntryId entryId, NetWorthEntryType type,
                                         NetWorthEntryName name, Money amount) {

    public ChangeNetWorthEntryCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
