package de.jcss.hbt.domain.networth.overview;

import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.Objects;

/**
 * A year of the {@link NetWorthHistory}.
 *
 * @param change {@code null} if the year before has no statement (VS-12)
 */
@QueryModel
public record NetWorthHistoryEntry(NetWorthTotals totals, NetWorthChange change) {

    public NetWorthHistoryEntry {
        Objects.requireNonNull(totals, "totals must not be null");
    }
}
