package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.networth.NetWorthStatementId;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/** A user recorded the first asset or liability for a year, which so gets a net worth statement (VS-7). */
public record NetWorthStatementCreated(NetWorthStatementId statementId, OwnerId owner, Year year,
                                       Instant timestamp) implements NetWorthStatementEvent {

    public NetWorthStatementCreated {
        Objects.requireNonNull(statementId, "statementId must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
