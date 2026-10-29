package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthStatementId;

import java.time.Instant;
import java.util.Objects;

/** The type, name or amount of an asset or a liability was changed (VS-5). */
public record NetWorthEntryChanged(NetWorthStatementId statementId, NetWorthEntryId entryId,
                                   Instant timestamp) implements NetWorthStatementEvent {

    public NetWorthEntryChanged {
        Objects.requireNonNull(statementId, "statementId must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
