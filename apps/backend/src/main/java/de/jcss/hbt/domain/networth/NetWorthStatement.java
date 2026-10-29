package de.jcss.hbt.domain.networth;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.NetWorthEntryNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.event.NetWorthEntryAdded;
import de.jcss.hbt.domain.networth.event.NetWorthEntryChanged;
import de.jcss.hbt.domain.networth.event.NetWorthEntryRemoved;
import de.jcss.hbt.domain.networth.event.NetWorthStatementCopied;
import de.jcss.hbt.domain.networth.event.NetWorthStatementCreated;
import de.jcss.hbt.domain.networth.event.NetWorthStatementEvent;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import de.jcss.hbt.domain.networth.valueobject.NetWorthStatementId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root holding the assets and liabilities ({@link NetWorthEntry}s) of a user at the end of
 * one year (VS-1, VS-2), the Vermögensstand. It belongs to the user who recorded it, its
 * {@link OwnerId owner}; only they may see or change it, see {@link #isOwnedBy}. It is independent of
 * the household plan of the same year (VS-4).
 * <p>
 * A year has a statement as long as it has at least one entry (VS-7): the application layer creates it
 * with the first entry and removes it once {@link #isEmpty()} after the last one is gone. The entries
 * are held in the order they were recorded, the newest last (VS-8).
 * <p>
 * That a user has at most one statement per year spans several aggregates and is therefore ensured by
 * the application layer and the repository, not by this class.
 * <p>
 * Identity (equals/hashCode) is based solely on the statement id.
 */
@AggregateRoot
public final class NetWorthStatement {

    /** The first year a statement may be recorded for, as for household plans (VS-2). */
    public static final int FIRST_YEAR = 1900;
    /** The last year a statement may be recorded for, as for household plans (VS-2). */
    public static final int LAST_YEAR = 2200;

    @Identity
    private final NetWorthStatementId id;
    private final OwnerId owner;
    private final Year year;
    private final List<NetWorthEntry> entries;
    private final List<NetWorthStatementEvent> domainEvents = new ArrayList<>();

    private NetWorthStatement(NetWorthStatementId id, OwnerId owner, Year year, List<NetWorthEntry> entries) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.year = Objects.requireNonNull(year, "year must not be null");
        this.entries = new ArrayList<>(requireDistinctEntries(entries));
    }

    private static List<NetWorthEntry> requireDistinctEntries(List<NetWorthEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        Set<NetWorthEntryId> ids = new HashSet<>();
        for (NetWorthEntry entry : entries) {
            Objects.requireNonNull(entry, "entries must not contain null");
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("entries must not contain %s twice".formatted(entry.id()));
            }
        }
        return entries;
    }

    /**
     * Starts a new, still empty statement of the given owner for the given year; the caller adds the
     * first entry right away (VS-7).
     *
     * @throws DomainValidationException if the year is before {@value #FIRST_YEAR} or after {@value #LAST_YEAR}
     */
    public static NetWorthStatement create(NetWorthStatementId id, OwnerId owner, Year year) {
        requireYearInRange(year);
        NetWorthStatement statement = new NetWorthStatement(id, owner, year, List.of());
        statement.record(new NetWorthStatementCreated(id, owner, year, Instant.now()));
        return statement;
    }

    /** @throws DomainValidationException if the year is before {@value #FIRST_YEAR} or after {@value #LAST_YEAR} */
    public static void requireYearInRange(Year year) {
        Objects.requireNonNull(year, "year must not be null");
        if (year.getValue() < FIRST_YEAR || year.getValue() > LAST_YEAR) {
            throw new DomainValidationException(
                    "year must be between %d and %d, but was %s".formatted(FIRST_YEAR, LAST_YEAR, year));
        }
    }

    /**
     * Starts a new statement of the owner of the source for the given year, holding copies of all
     * entries of the source in their order (VS-15). The source stays unchanged.
     *
     * @throws DomainValidationException if the year is out of range or the year of the source
     */
    public static NetWorthStatement createFrom(NetWorthStatement source, NetWorthStatementId id, Year year) {
        Objects.requireNonNull(source, "source must not be null");
        NetWorthStatement statement = create(id, source.owner, year);
        statement.takeOver(source, false);
        return statement;
    }

    /**
     * Replaces all entries of this statement by copies of those of the source (VS-16). The statement
     * keeps its id, owner and year.
     *
     * @throws DomainValidationException if the source has the same year
     * @throws IllegalArgumentException  if the source belongs to someone else
     */
    public void overwriteWith(NetWorthStatement source) {
        Objects.requireNonNull(source, "source must not be null");
        takeOver(source, true);
    }

    private void takeOver(NetWorthStatement source, boolean overwritten) {
        if (!source.isOwnedBy(owner)) {
            throw new IllegalArgumentException("source %s belongs to someone else".formatted(source.id));
        }
        source.requireCopyableTo(year);
        entries.clear();
        source.entries.forEach(entry -> entries.add(entry.copy()));
        record(new NetWorthStatementCopied(id, source.id, overwritten, Instant.now()));
    }

    /**
     * Checks that this statement may be copied into the given year, before the statement of that year is looked up.
     *
     * @throws DomainValidationException if the year is out of range or the year of this statement (VS-15)
     */
    public void requireCopyableTo(Year target) {
        requireYearInRange(target);
        if (year.equals(target)) {
            throw new DomainValidationException(
                    "the target year must differ from the year of the source, %s".formatted(target));
        }
    }

    /** Rebuilds a stored statement, e.g. in a repository; records no events. */
    public static NetWorthStatement reconstitute(NetWorthStatementId id, OwnerId owner, Year year,
                                                 List<NetWorthEntry> entries) {
        return new NetWorthStatement(id, owner, year, entries);
    }

    /** Whether the statement belongs to the given user, i.e. whether they may see and change it. */
    public boolean isOwnedBy(OwnerId user) {
        return owner.equals(user);
    }

    /** Records an asset or a liability at the bottom of its list (VS-5, VS-8). */
    public NetWorthEntryId addEntry(NetWorthEntryType type, NetWorthEntryName name, Money amount) {
        NetWorthEntry entry = NetWorthEntry.of(NetWorthEntryId.generate(), type, name, amount);
        entries.addLast(entry);
        record(new NetWorthEntryAdded(id, entry.id(), Instant.now()));
        return entry.id();
    }

    /**
     * Gives an entry new values while it keeps its id and its position (VS-5). Records an event only if
     * a value actually changed.
     *
     * @throws NetWorthEntryNotFoundException if the entry is not part of this statement
     */
    public void changeEntry(NetWorthEntryId entryId, NetWorthEntryType type, NetWorthEntryName name, Money amount) {
        int index = indexOf(entryId);
        NetWorthEntry current = entries.get(index);
        NetWorthEntry changed = current.changedTo(type, name, amount);
        if (changed.hasSameValuesAs(current)) {
            return;
        }
        entries.set(index, changed);
        record(new NetWorthEntryChanged(id, entryId, Instant.now()));
    }

    /**
     * Removes an entry (VS-5). Removing the last one leaves the statement {@link #isEmpty() empty}.
     *
     * @throws NetWorthEntryNotFoundException if the entry is not part of this statement
     */
    public void removeEntry(NetWorthEntryId entryId) {
        entries.remove(indexOf(entryId));
        record(new NetWorthEntryRemoved(id, entryId, Instant.now()));
    }

    private int indexOf(NetWorthEntryId entryId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).hasId(entryId)) {
                return i;
            }
        }
        throw new NetWorthEntryNotFoundException(entryId);
    }

    /** Whether the statement has no entry left, so the year no longer has a statement (VS-7). */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the events recorded since the last call and forgets them, so that each event is published
     * exactly once.
     */
    public List<NetWorthStatementEvent> pullDomainEvents() {
        List<NetWorthStatementEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    private void record(NetWorthStatementEvent event) {
        domainEvents.add(event);
    }

    public NetWorthStatementId id() {
        return id;
    }

    public OwnerId owner() {
        return owner;
    }

    public Year year() {
        return year;
    }

    /** The entries in the order they were recorded, the oldest first (VS-8). */
    public List<NetWorthEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetWorthStatement other)) {
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
        return "NetWorthStatement{id=%s, owner=%s, year=%s, entries=%d}".formatted(id, owner.value(), year,
                entries.size());
    }
}
