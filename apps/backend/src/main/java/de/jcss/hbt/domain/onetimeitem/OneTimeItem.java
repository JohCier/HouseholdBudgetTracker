package de.jcss.hbt.domain.onetimeitem;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemChanged;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemCreated;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemDeleted;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemEvent;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root of a single income or expense that happens once, on a given date, such as a
 * back payment (EP-1). Unlike a {@code BudgetItem} it is not part of a household plan: it
 * belongs to its owner directly and exists for a year whether or not the owner has a plan for
 * it (EP-5). Its amount is the amount of this one event, never spread over months (EP-2). The
 * year it counts for is the year of its date (EP-3).
 * <p>
 * Only its {@link OwnerId owner} may see, change or delete it, see {@link #isOwnedBy} (EP-4).
 * All changes are recorded as {@link OneTimeItemEvent}s, which the application layer takes via
 * {@link #pullDomainEvents()} after saving to publish them.
 * <p>
 * Identity (equals/hashCode) is based solely on the item id.
 */
@AggregateRoot
public final class OneTimeItem {

    /** The first day a one-time item may have, as the first year of a plan (EP-3, HP-1). */
    public static final LocalDate FIRST_DATE = LocalDate.of(1900, 1, 1);
    /** The last day a one-time item may have, as the last year of a plan (EP-3, HP-1). */
    public static final LocalDate LAST_DATE = LocalDate.of(2200, 12, 31);

    @Identity
    private final OneTimeItemId id;
    private final OwnerId owner;
    private final ItemType type;
    private final BudgetItemDescription description;
    private final Money amount;
    private final LocalDate date;
    private final BudgetItemNote note;
    private final List<OneTimeItemEvent> domainEvents = new ArrayList<>();

    private OneTimeItem(OneTimeItemId id, OwnerId owner, ItemType type, BudgetItemDescription description,
                        Money amount, LocalDate date, BudgetItemNote note) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.note = note;
        if (!amount.isPositive()) {
            throw new DomainValidationException("amount must be positive");
        }
        if (date.isBefore(FIRST_DATE) || date.isAfter(LAST_DATE)) {
            throw new DomainValidationException(
                    "date must be between %s and %s, but was %s".formatted(FIRST_DATE, LAST_DATE, date));
        }
    }

    /**
     * Enters a new one-time item of the owner.
     *
     * @param note {@code null} if the item has no note
     * @throws DomainValidationException if the amount is not positive or the date out of range (EP-1, EP-3)
     */
    public static OneTimeItem create(OneTimeItemId id, OwnerId owner, ItemType type,
                                     BudgetItemDescription description, Money amount, LocalDate date,
                                     BudgetItemNote note) {
        OneTimeItem item = new OneTimeItem(id, owner, type, description, amount, date, note);
        item.record(new OneTimeItemCreated(id, owner, date, Instant.now()));
        return item;
    }

    /** Rebuilds a stored item, e.g. in a repository; checks the same rules, records no events. */
    public static OneTimeItem reconstitute(OneTimeItemId id, OwnerId owner, ItemType type,
                                           BudgetItemDescription description, Money amount, LocalDate date,
                                           BudgetItemNote note) {
        return new OneTimeItem(id, owner, type, description, amount, date, note);
    }

    /** Whether the item belongs to the given user, i.e. whether they may see and change it (EP-4). */
    public boolean isOwnedBy(OwnerId user) {
        return owner.equals(user);
    }

    /**
     * Replaces everything the user entered (EP-7). Returns this item with the new values, which
     * is to be saved, since all fields are final; pending events are carried over. With a date in
     * another year, the item counts for that year from now on.
     *
     * @param note {@code null} if the item has no note any more
     * @throws DomainValidationException if the amount is not positive or the date out of range (EP-1, EP-3)
     */
    public OneTimeItem change(ItemType type, BudgetItemDescription description, Money amount, LocalDate date,
                              BudgetItemNote note) {
        OneTimeItem changed = new OneTimeItem(id, owner, type, description, amount, date, note);
        changed.domainEvents.addAll(domainEvents);
        changed.record(new OneTimeItemChanged(id, date, Instant.now()));
        return changed;
    }

    /** Marks the item as deleted (EP-17); removing it from its repository is up to the caller. */
    public void delete() {
        record(new OneTimeItemDeleted(id, Instant.now()));
    }

    /**
     * Returns the events recorded since the last call and forgets them, so that each
     * event is published exactly once.
     */
    public List<OneTimeItemEvent> pullDomainEvents() {
        List<OneTimeItemEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    private void record(OneTimeItemEvent event) {
        domainEvents.add(event);
    }

    public OneTimeItemId id() {
        return id;
    }

    public OwnerId owner() {
        return owner;
    }

    public ItemType type() {
        return type;
    }

    public BudgetItemDescription description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate date() {
        return date;
    }

    /** The year the item counts for: the year of its date (EP-3). */
    public Year year() {
        return Year.of(date.getYear());
    }

    /** {@code null} if the item has no note. */
    public BudgetItemNote note() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OneTimeItem other)) {
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
        return "OneTimeItem{id=%s, owner=%s, type=%s, date=%s}".formatted(id, owner.value(), type, date);
    }
}
