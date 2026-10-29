package de.jcss.hbt.domain.networth.valueobject;

import de.jcss.hbt.domain.networth.NetWorthEntry;
import org.jmolecules.ddd.annotation.ValueObject;

/** Whether a {@link NetWorthEntry} is something the user owns or something they owe (VS-1). */
@ValueObject
public enum NetWorthEntryType {
    /** Vermögenswert, e.g. "Girokonto"; adds to the net worth. */
    ASSET,
    /** Schuld, e.g. "Autokredit"; taken off the net worth. */
    LIABILITY
}
