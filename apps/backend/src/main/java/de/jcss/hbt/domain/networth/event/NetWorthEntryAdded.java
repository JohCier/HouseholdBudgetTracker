package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthStatementId;

import java.time.Instant;
import java.util.Objects;

/** An asset or a liability was recorded for a year (VS-5). */
public record NetWorthEntryAdded(NetWorthStatementId statementId, NetWorthEntryId entryId,
                                 Instant timestamp) implements NetWorthStatementEvent {

    public NetWorthEntryAdded {
        Objects.requireNonNull(statementId, "statementId must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
