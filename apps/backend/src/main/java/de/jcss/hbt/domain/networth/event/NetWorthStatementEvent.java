package de.jcss.hbt.domain.networth.event;

import de.jcss.hbt.domain.networth.NetWorthStatementId;
import de.jcss.hbt.domain.shared.TimestampedDomainEvent;

/** A domain event about a change to the net worth statement of one year. */
public interface NetWorthStatementEvent extends TimestampedDomainEvent {

    /** The statement that was changed. */
    NetWorthStatementId statementId();
}
