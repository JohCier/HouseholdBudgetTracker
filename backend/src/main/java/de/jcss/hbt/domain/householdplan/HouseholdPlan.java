package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import de.jcss.hbt.domain.shared.AggregateRoot;
import de.jcss.hbt.domain.shared.DomainEvent;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root holding the income and expense positions ({@link BudgetItem}s) planned
 * for one year. All changes to the items go through this class, which records each of
 * them as a {@link DomainEvent}. The recorded events are collected until the application
 * layer saves the plan and takes them via {@link #pullDomainEvents()} to publish them.
 * <p>
 * The items can be read from outside (e.g. by a repository to store them), but only be
 * changed through this class: the list handed out is unmodifiable, the items themselves
 * are immutable.
 * <p>
 * That there is at most one plan per year spans several aggregates and is therefore
 * ensured by the application layer, not by this class.
 * <p>
 * Identity (equals/hashCode) is based solely on the plan id.
 */
@AggregateRoot
public final class HouseholdPlan {

    private final HouseholdPlanId id;
    private final Year year;
    private final List<BudgetItem> items;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private HouseholdPlan(HouseholdPlanId id, Year year, List<BudgetItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.year = Objects.requireNonNull(year, "year must not be null");
        this.items = new ArrayList<>(items);
    }

    /** Starts a new, empty plan for the given year. */
    public static HouseholdPlan create(HouseholdPlanId id, Year year) {
        HouseholdPlan plan = new HouseholdPlan(id, year, List.of());
        plan.record(new HouseholdPlanCreated(id, year));
        return plan;
    }

    /** Rebuilds a stored plan, e.g. in a repository; records no events. */
    public static HouseholdPlan reconstitute(HouseholdPlanId id, Year year, List<BudgetItem> items) {
        return new HouseholdPlan(id, year, items);
    }

    public BudgetItemId addIncome(BudgetItemDescription description, Money amount) {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), description, amount);
        items.add(item);
        record(new IncomeAdded(id, item.id()));
        return item.id();
    }

    public BudgetItemId addExpense(BudgetItemDescription description, Money amount, ExpenseCategory category) {
        BudgetItem item = BudgetItem.expense(BudgetItemId.generate(), description, amount, category);
        items.add(item);
        record(new ExpenseAdded(id, item.id()));
        return item.id();
    }

    /**
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     */
    public void removeItem(BudgetItemId itemId) {
        if (!items.removeIf(item -> item.hasId(itemId))) {
            throw new BudgetItemNotFoundException(itemId);
        }
        record(new BudgetItemRemoved(id, itemId));
    }

    /**
     * Returns the events recorded since the last call and forgets them, so that each
     * event is published exactly once.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    private void record(DomainEvent event) {
        domainEvents.add(event);
    }

    public HouseholdPlanId id() {
        return id;
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
        return "HouseholdPlan{id=%s, year=%s, items=%d}".formatted(id, year, items.size());
    }
}
