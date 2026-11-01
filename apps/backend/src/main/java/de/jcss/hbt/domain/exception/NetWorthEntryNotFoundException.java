package de.jcss.hbt.domain.exception;

import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;

/**
 * Thrown when a {@code NetWorthEntry} is referenced by an id that does not exist in the statement of the year.
 */
public class NetWorthEntryNotFoundException extends NotFoundException {

    private static final String CODE = "net-worth-entry-not-found";

    public NetWorthEntryNotFoundException(NetWorthEntryId id) {
        super(CODE, "Net worth entry not found: " + id.value());
    }
}
