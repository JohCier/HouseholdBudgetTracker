package de.jcss.hbt.domain.networth.command;

import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/** Record an asset or a liability of the given owner at the end of a year (VS-1, VS-2, VS-5). */
@Command
public record AddNetWorthEntryCommand(OwnerId owner, Year year, NetWorthEntryType type, NetWorthEntryName name,
                                      Money amount) {

    public AddNetWorthEntryCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
