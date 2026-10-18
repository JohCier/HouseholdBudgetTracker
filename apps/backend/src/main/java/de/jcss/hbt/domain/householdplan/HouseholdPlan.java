package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemChanged;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanEvent;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
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
 * Aggregate root holding the income and expense positions ({@link BudgetItem}s) a user
 * plans for one year. The plan belongs to the user who created it, its {@link OwnerId owner};
 * only they may see or change it, see {@link #isOwnedBy}. All changes to the items go through this class, which records each of
 * them as a {@link HouseholdPlanEvent}. The recorded events are collected until the application
 * layer saves the plan and takes them via {@link #pullDomainEvents()} to publish them.
 * <p>
 * The items can be read from outside (e.g. by a repository to store them), but only be
 * changed through this class: the list handed out is unmodifiable, the items themselves
 * are immutable.
 * <p>
 * That a user has at most one plan per year spans several aggregates and is therefore
 * ensured by the application layer, not by this class.
 * <p>
 * Identity (equals/hashCode) is based solely on the plan id.
 */
@AggregateRoot
public final class HouseholdPlan {

    @Identity
    private final HouseholdPlanId id;
    private final OwnerId owner;
    private final Year year;
    private final List<BudgetItem> items;
    private final List<HouseholdPlanEvent> domainEvents = new ArrayList<>();

    private HouseholdPlan(HouseholdPlanId id, OwnerId owner, Year year, List<BudgetItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.year = Objects.requireNonNull(year, "year must not be null");
        this.items = new ArrayList<>(requireDistinctItems(items));
    }

    private static List<BudgetItem> requireDistinctItems(List<BudgetItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        Set<BudgetItemId> ids = new HashSet<>();
        for (BudgetItem item : items) {
            Objects.requireNonNull(item, "items must not contain null");
            if (!ids.add(item.id())) {
                throw new IllegalArgumentException("items must not contain %s twice".formatted(item.id()));
            }
        }
        return items;
    }

    /** Starts a new, empty plan of the given owner for the given year. */
    public static HouseholdPlan create(HouseholdPlanId id, OwnerId owner, Year year) {
        HouseholdPlan plan = new HouseholdPlan(id, owner, year, List.of());
        plan.record(new HouseholdPlanCreated(id, owner, year, Instant.now()));
        return plan;
    }

    /** Rebuilds a stored plan, e.g. in a repository; records no events. */
    public static HouseholdPlan reconstitute(HouseholdPlanId id, OwnerId owner, Year year, List<BudgetItem> items) {
        return new HouseholdPlan(id, owner, year, items);
    }

    /** Whether the plan belongs to the given user, i.e. whether they may see and change it. */
    public boolean isOwnedBy(OwnerId user) {
        return owner.equals(user);
    }

    public BudgetItemId addIncome(BudgetItemDescription description, Money amount) {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), description, amount);
        items.add(item);
        record(new IncomeAdded(id, item.id(), Instant.now()));
        return item.id();
    }

    public BudgetItemId addExpense(BudgetItemDescription description, Money amount, ExpenseCategory category) {
        BudgetItem item = BudgetItem.expense(BudgetItemId.generate(), description, amount, category);
        items.add(item);
        record(new ExpenseAdded(id, item.id(), Instant.now()));
        return item.id();
    }

    /**
     * Gives an item new values while it keeps its id and its position among the items.
     * Records an event only if a value actually changed.
     *
     * @param category the new category of an expense; {@code null} for an income
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     */
    public void changeItem(BudgetItemId itemId, BudgetItemDescription description, Money amount,
                           ExpenseCategory category) {
        int index = indexOf(itemId);
        BudgetItem current = items.get(index);
        BudgetItem changed = current.changedTo(description, amount, category);
        if (changed.hasSameValuesAs(current)) {
            return;
        }
        items.set(index, changed);
        record(new BudgetItemChanged(id, itemId, Instant.now()));
    }

    private int indexOf(BudgetItemId itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).hasId(itemId)) {
                return i;
            }
        }
        throw new BudgetItemNotFoundException(itemId);
    }

    /**
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     */
    public void removeItem(BudgetItemId itemId) {
        if (!items.removeIf(item -> item.hasId(itemId))) {
            throw new BudgetItemNotFoundException(itemId);
        }
        record(new BudgetItemRemoved(id, itemId, Instant.now()));
    }

    /** Income, expenses per category and balance of all items of this plan. */
    public BudgetSummary summary() {
        BudgetSummary summary = BudgetSummary.EMPTY;
        for (BudgetItem item : items) {
            summary = item.addTo(summary);
        }
        return summary;
    }

    /**
     * Returns the events recorded since the last call and forgets them, so that each
     * event is published exactly once.
     */
    public List<HouseholdPlanEvent> pullDomainEvents() {
        List<HouseholdPlanEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    private void record(HouseholdPlanEvent event) {
        domainEvents.add(event);
    }

    public HouseholdPlanId id() {
        return id;
    }

    public OwnerId owner() {
        return owner;
    }

    public Year year() {
        return year;
    }

    /** The items in the order they were added. */
    public List<BudgetItem> items() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HouseholdPlan other)) {
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
        return "HouseholdPlan{id=%s, owner=%s, year=%s, items=%d}".formatted(id, owner.value(), year, items.size());
    }
}
