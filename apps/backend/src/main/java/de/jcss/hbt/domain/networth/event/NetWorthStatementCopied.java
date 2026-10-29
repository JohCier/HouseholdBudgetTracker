package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.networth.NetWorthStatementId;

import java.time.Instant;
import java.util.Objects;

/**
 * A net worth statement got copies of all entries of the statement of another year of the same user, the source (VS-15).
 *
 * @param overwritten whether the statement existed before and its entries were replaced; otherwise it was just created
 */
public record NetWorthStatementCopied(NetWorthStatementId statementId, NetWorthStatementId sourceStatementId,
                                      boolean overwritten, Instant timestamp) implements NetWorthStatementEvent {

    public NetWorthStatementCopied {
        Objects.requireNonNull(statementId, "statementId must not be null");
        Objects.requireNonNull(sourceStatementId, "sourceStatementId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
