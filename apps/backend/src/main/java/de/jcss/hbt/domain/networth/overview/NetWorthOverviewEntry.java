package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthEntryName;
import de.jcss.hbt.domain.networth.NetWorthEntryType;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.Objects;

/** An asset or a liability as listed on the page of its year (VS-8). */
@QueryModel
public record NetWorthOverviewEntry(NetWorthEntryId id, NetWorthEntryType type, NetWorthEntryName name, Money amount) {

    public NetWorthOverviewEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
