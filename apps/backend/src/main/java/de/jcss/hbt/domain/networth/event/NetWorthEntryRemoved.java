package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthStatementId;

import java.time.Instant;
import java.util.Objects;

/** An asset or a liability was removed (VS-5). */
public record NetWorthEntryRemoved(NetWorthStatementId statementId, NetWorthEntryId entryId,
                                   Instant timestamp) implements NetWorthStatementEvent {

    public NetWorthEntryRemoved {
        Objects.requireNonNull(statementId, "statementId must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
