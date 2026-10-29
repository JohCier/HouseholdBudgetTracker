package de.jcss.hbt.domain.networth;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

/**
 * An asset or a liability of a {@link NetWorthStatement}, with its name and its amount at the end of
 * the year (VS-1, VS-2). The amount is always positive, also for a liability; the type decides whether
 * it adds to the net worth or is taken off it.
 * <p>
 * Immutable: changes give a new entry with the same id. Only the statement creates and changes its
 * entries. Identity (equals/hashCode) is based solely on the entry id.
 */
@Entity
public final class NetWorthEntry {

    @Identity
    private final NetWorthEntryId id;
    private final NetWorthEntryType type;
    private final NetWorthEntryName name;
    private final Money amount;

    private NetWorthEntry(NetWorthEntryId id, NetWorthEntryType type, NetWorthEntryName name, Money amount) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new DomainValidationException("amount must be greater than 0");
        }
        this.amount = amount;
    }

    static NetWorthEntry of(NetWorthEntryId id, NetWorthEntryType type, NetWorthEntryName name, Money amount) {
        return new NetWorthEntry(id, type, name, amount);
    }

    /** Rebuilds a stored entry, e.g. in a repository. */
    public static NetWorthEntry reconstitute(NetWorthEntryId id, NetWorthEntryType type, NetWorthEntryName name,
                                             Money amount) {
        return new NetWorthEntry(id, type, name, amount);
    }

    /** The same entry with new values. */
    NetWorthEntry changedTo(NetWorthEntryType type, NetWorthEntryName name, Money amount) {
        return new NetWorthEntry(id, type, name, amount);
    }

    /** An independent copy with a new id, e.g. for another year (VS-15). */
    NetWorthEntry copy() {
        return new NetWorthEntry(NetWorthEntryId.generate(), type, name, amount);
    }

    boolean hasId(NetWorthEntryId other) {
        return id.equals(other);
    }

    boolean hasSameValuesAs(NetWorthEntry other) {
        return type == other.type && name.equals(other.name) && amount.equals(other.amount);
    }

    public NetWorthEntryId id() {
        return id;
    }

    public NetWorthEntryType type() {
        return type;
    }

    public NetWorthEntryName name() {
        return name;
    }

    public Money amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetWorthEntry other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "NetWorthEntry{id=%s, type=%s, name=%s, amount=%s}".formatted(id.value(), type, name, amount.value());
    }
}
