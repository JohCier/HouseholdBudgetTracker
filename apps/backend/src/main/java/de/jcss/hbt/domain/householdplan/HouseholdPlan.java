package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemChanged;
import de.jcss.hbt.domain.householdplan.event.BudgetItemMarked;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.BudgetItemsReordered;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCopied;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanDeleted;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanEvent;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import de.jcss.hbt.domain.householdplan.event.SubcategoriesReordered;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMark;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import de.jcss.hbt.domain.householdplan.valueobject.SubcategoryOrder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * The plan also keeps the order in which its entries are shown (UK-14 to UK-18): the items are
 * held in display order, the newest last unless the user moved them (EG-1); the order of the
 * subcategory groups of each {@link ItemSection} is a {@link SubcategoryOrder}. An item always
 * takes the spelling of the subcategory other items of the plan already have, so all items
 * of a group are spelled alike (UK-5).
 * <p>
 * That a user has at most one plan per year spans several aggregates and is therefore
 * ensured by the application layer, not by this class.
 * <p>
 * New plans are only created for the years {@value #FIRST_YEAR} to {@value #LAST_YEAR}. Stored plans
 * of other years are still reconstituted, so that existing data stays readable.
 * <p>
 * Identity (equals/hashCode) is based solely on the plan id.
 */
@AggregateRoot
public final class HouseholdPlan {

    /** The first year a new plan may be created for. */
    public static final int FIRST_YEAR = 1900;
    /** The last year a new plan may be created for. */
    public static final int LAST_YEAR = 2200;

    @Identity
    private final HouseholdPlanId id;
    private final OwnerId owner;
    private final Year year;
    private final List<BudgetItem> items;
    private final Map<ItemSection, SubcategoryOrder> subcategoryOrders;
    private final List<HouseholdPlanEvent> domainEvents = new ArrayList<>();

    private HouseholdPlan(HouseholdPlanId id, OwnerId owner, Year year, List<BudgetItem> items,
                          Map<ItemSection, SubcategoryOrder> subcategoryOrders) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.year = Objects.requireNonNull(year, "year must not be null");
        this.items = new ArrayList<>(requireDistinctItems(items));
        this.subcategoryOrders = new HashMap<>(
                Objects.requireNonNull(subcategoryOrders, "subcategoryOrders must not be null"));
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

    /**
     * Starts a new, empty plan of the given owner for the given year.
     *
     * @throws DomainValidationException if the year is before {@value #FIRST_YEAR} or after {@value #LAST_YEAR}
     */
    public static HouseholdPlan create(HouseholdPlanId id, OwnerId owner, Year year) {
        Objects.requireNonNull(year, "year must not be null");
        if (year.getValue() < FIRST_YEAR || year.getValue() > LAST_YEAR) {
            throw new DomainValidationException(
                    "year must be between %d and %d, but was %s".formatted(FIRST_YEAR, LAST_YEAR, year));
        }
        HouseholdPlan plan = new HouseholdPlan(id, owner, year, List.of(), Map.of());
        plan.record(new HouseholdPlanCreated(id, owner, year, Instant.now()));
        return plan;
    }

    /**
     * Starts a new plan of the owner of the source for the given year, holding copies of all
     * items of the source in their order (PU-3, PU-4, PU-8). The source stays unchanged (PU-5).
     *
     * @throws DomainValidationException if the year is out of range or the year of the source (PU-2)
     */
    public static HouseholdPlan createFrom(HouseholdPlan source, HouseholdPlanId id, Year year) {
        Objects.requireNonNull(source, "source must not be null");
        HouseholdPlan plan = create(id, source.owner, year);
        plan.takeOver(source, false);
        return plan;
    }

    /**
     * Replaces all items of this plan and the order of their groups by copies of those of the
     * source (PU-9). The plan keeps its id, owner and year.
     *
     * @throws DomainValidationException if the source is this plan, i.e. has the same year (PU-2)
     * @throws IllegalArgumentException  if the source belongs to someone else
     */
    public void overwriteWith(HouseholdPlan source) {
        Objects.requireNonNull(source, "source must not be null");
        takeOver(source, true);
    }

    private void takeOver(HouseholdPlan source, boolean overwritten) {
        if (!source.isOwnedBy(owner)) {
            throw new IllegalArgumentException("source %s belongs to someone else".formatted(source.id));
        }
        if (source.year.equals(year)) {
            throw new DomainValidationException(
                    "the target year must differ from the year of the source, %s".formatted(year));
        }
        items.clear();
        source.items.forEach(item -> items.add(item.copy()));
        subcategoryOrders.clear();
        subcategoryOrders.putAll(source.subcategoryOrders);
        record(new HouseholdPlanCopied(id, source.id, overwritten, Instant.now()));
    }

    /**
     * Marks the plan as deleted with all its items (PL-2, PL-6): records a single event naming the
     * number of items, none per item. Removing the plan from its repository is up to the caller.
     */
    public void delete() {
        record(new HouseholdPlanDeleted(id, year, items.size(), Instant.now()));
    }

    /**
     * Rebuilds a stored plan, e.g. in a repository; records no events.
     *
     * @param items             in display order, the topmost first
     * @param subcategoryOrders the arranged order of the groups per section; sections never arranged are missing
     */
    public static HouseholdPlan reconstitute(HouseholdPlanId id, OwnerId owner, Year year, List<BudgetItem> items,
                                             Map<ItemSection, SubcategoryOrder> subcategoryOrders) {
        return new HouseholdPlan(id, owner, year, items, subcategoryOrders);
    }

    /** Whether the plan belongs to the given user, i.e. whether they may see and change it. */
    public boolean isOwnedBy(OwnerId user) {
        return owner.equals(user);
    }

    /**
     * Adds an income without marks at the bottom of its group (EG-1, PM-1).
     *
     * @param subcategory {@code null} if the income has no subcategory
     * @param note        {@code null} if the income has no note
     */
    public BudgetItemId addIncome(BudgetItemDescription description, ItemAmount amount,
                                  BudgetItemSubcategory subcategory, BudgetItemNote note) {
        return addIncome(description, amount, subcategory, note, ItemMarks.NONE);
    }

    /**
     * Adds an income with the given marks at the bottom of its group, e.g. one that is restored (EG-3, PM-6).
     *
     * @param subcategory {@code null} if the income has no subcategory
     * @param note        {@code null} if the income has no note
     * @throws DomainValidationException if the marks name a saving potential (PM-2)
     */
    public BudgetItemId addIncome(BudgetItemDescription description, ItemAmount amount,
                                  BudgetItemSubcategory subcategory, BudgetItemNote note, ItemMarks marks) {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), description, amount,
                spellingOf(subcategory, null), note).withMarks(marks);
        items.addLast(item);
        record(new IncomeAdded(id, item.id(), Instant.now()));
        return item.id();
    }

    /**
     * Adds an expense without marks at the bottom of its group (EG-1, PM-1, PM-2).
     *
     * @param subcategory {@code null} if the expense has no subcategory
     * @param note        {@code null} if the expense has no note
     */
    public BudgetItemId addExpense(BudgetItemDescription description, ItemAmount amount, ExpenseCategory category,
                                   BudgetItemSubcategory subcategory, BudgetItemNote note) {
        return addExpense(description, amount, category, subcategory, note, ItemMarks.NONE);
    }

    /**
     * Adds an expense with the given marks at the bottom of its group, e.g. one that is restored (EG-3, PM-6).
     *
     * @param subcategory {@code null} if the expense has no subcategory
     * @param note        {@code null} if the expense has no note
     * @throws DomainValidationException if the saving potential exceeds the monthly amount (PM-2a)
     */
    public BudgetItemId addExpense(BudgetItemDescription description, ItemAmount amount, ExpenseCategory category,
                                   BudgetItemSubcategory subcategory, BudgetItemNote note, ItemMarks marks) {
        BudgetItem item = BudgetItem.expense(BudgetItemId.generate(), description, amount, category,
                spellingOf(subcategory, null), note).withMarks(marks);
        items.addLast(item);
        record(new ExpenseAdded(id, item.id(), Instant.now()));
        return item.id();
    }

    /**
     * Gives an item new values while it keeps its id and its marks; a saving potential above the new
     * monthly amount is lowered to it (PM-5). It keeps its position among the items as
     * long as it stays in its group; moved to another group, it goes to the bottom of it (PB-4, EG-2).
     * Records an event only if a value actually changed.
     *
     * @param category    the new category of an expense; {@code null} for an income
     * @param subcategory the new subcategory; {@code null} to remove it
     * @param note        the new note; {@code null} to remove it
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     */
    public void changeItem(BudgetItemId itemId, BudgetItemDescription description, ItemAmount amount,
                           ExpenseCategory category, BudgetItemSubcategory subcategory, BudgetItemNote note) {
        int index = indexOf(itemId);
        BudgetItem current = items.get(index);
        BudgetItem changed = current.changedTo(description, amount, category, spellingOf(subcategory, itemId), note);
        if (changed.hasSameValuesAs(current)) {
            return;
        }
        if (changed.isInSameGroupAs(current)) {
            items.set(index, changed);
        } else {
            items.remove(index);
            items.addLast(changed);
            forgetVanishedGroups();
        }
        record(new BudgetItemChanged(id, itemId, Instant.now()));
    }

    /**
     * Marks an item as important or no longer (PM-1, PM-4a). Its position stays (PM-7). Records an
     * event only if the mark actually changed.
     *
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     */
    public void markImportant(BudgetItemId itemId, boolean important) {
        int index = indexOf(itemId);
        BudgetItem current = items.get(index);
        if (current.marks().important() == important) {
            return;
        }
        items.set(index, current.withMarks(current.marks().withImportant(important)));
        record(new BudgetItemMarked(id, itemId, ItemMark.IMPORTANT, important, Instant.now()));
    }

    /**
     * Sets, changes or removes the saving potential of an expense (PM-2, PM-4b). Its position stays
     * (PM-7). Records an event only if the saving potential actually changed.
     *
     * @param savingPotential {@code null} to remove it
     * @throws BudgetItemNotFoundException if the item is not part of this plan
     * @throws DomainValidationException   if the item is an income, or the saving potential exceeds its monthly amount (PM-2a)
     */
    public void changeSavingPotential(BudgetItemId itemId, SavingPotential savingPotential) {
        int index = indexOf(itemId);
        BudgetItem current = items.get(index);
        BudgetItem changed = current.withMarks(current.marks().withSavingPotential(savingPotential));
        if (changed.hasSameValuesAs(current)) {
            return;
        }
        items.set(index, changed);
        record(new BudgetItemMarked(id, itemId, ItemMark.SAVING_POTENTIAL, savingPotential != null, Instant.now()));
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
        forgetVanishedGroups();
        record(new BudgetItemRemoved(id, itemId, Instant.now()));
    }

    /**
     * Puts the items of one subcategory group in the given order (UK-14). The other items keep
     * their positions. Records an event only if the order actually changed.
     *
     * @param order all items of one group, each once, the topmost first
     * @throws BudgetItemNotFoundException if an item is not part of this plan
     * @throws DomainValidationException   if the items are not exactly those of one group
     */
    public void reorderItems(List<BudgetItemId> order) {
        Objects.requireNonNull(order, "order must not be null");
        if (order.isEmpty()) {
            throw new DomainValidationException("the order must name the items of a group");
        }
        BudgetItem first = items.get(indexOf(order.getFirst()));
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isInSameGroupAs(first)) {
                positions.add(i);
            }
        }
        List<BudgetItem> reordered = new ArrayList<>();
        for (BudgetItemId itemId : order) {
            BudgetItem item = items.get(indexOf(itemId));
            if (!item.isInSameGroupAs(first) || reordered.contains(item)) {
                throw new DomainValidationException("the order must name each item of one group once");
            }
            reordered.add(item);
        }
        if (reordered.size() != positions.size()) {
            throw new DomainValidationException("the order must name all items of the group");
        }
        boolean changed = false;
        for (int i = 0; i < positions.size(); i++) {
            changed |= !items.set(positions.get(i), reordered.get(i)).equals(reordered.get(i));
        }
        if (changed) {
            record(new BudgetItemsReordered(id, Instant.now()));
        }
    }

    /**
     * Puts the subcategory groups of a section in the given order (UK-14). Records an event only
     * if the order shown actually changed.
     *
     * @param order all groups of the section, each once, the topmost first
     * @throws DomainValidationException if the order does not name exactly the groups of the section
     */
    public void reorderSubcategories(ItemSection section, List<BudgetItemSubcategory> order) {
        Objects.requireNonNull(section, "section must not be null");
        SubcategoryOrder arranged = new SubcategoryOrder(order);
        List<BudgetItemSubcategory> groups = groupsOf(section);
        if (groups.size() != arranged.subcategories().size() || !groups.stream().allMatch(arranged::names)) {
            throw new DomainValidationException("the order must name all subcategories of the section");
        }
        boolean changed = !arranged.arrange(groups).equals(subcategoryOrderOf(section).arrange(groups));
        subcategoryOrders.put(section, arranged);
        if (changed) {
            record(new SubcategoriesReordered(id, Instant.now()));
        }
    }

    /** The subcategory groups the section has, each once, in the spelling of its items. */
    private List<BudgetItemSubcategory> groupsOf(ItemSection section) {
        List<BudgetItemSubcategory> groups = new ArrayList<>();
        for (BudgetItem item : items) {
            if (item.section().equals(section) && groups.stream().noneMatch(item.group()::isSameAs)) {
                groups.add(item.group());
            }
        }
        return groups;
    }

    /** Drops the groups that have no items any more from the arranged orders, so they count as new should they come back (UK-18). */
    private void forgetVanishedGroups() {
        for (ItemSection section : List.copyOf(subcategoryOrders.keySet())) {
            SubcategoryOrder remaining = subcategoryOrders.get(section).retainingOnly(groupsOf(section));
            if (remaining.isEmpty()) {
                subcategoryOrders.remove(section);
            } else {
                subcategoryOrders.put(section, remaining);
            }
        }
    }

    /**
     * The spelling an item gets for the given subcategory: that of another item of the plan with
     * the same subcategory if there is one, so a group is spelled alike (UK-5); "Sonstiges" always
     * as {@link BudgetItemSubcategory#OTHER}.
     *
     * @param itemId the item the subcategory is for, which does not count as another; {@code null} for a new one
     */
    private BudgetItemSubcategory spellingOf(BudgetItemSubcategory subcategory, BudgetItemId itemId) {
        if (subcategory == null || subcategory.isOther()) {
            return subcategory == null ? null : BudgetItemSubcategory.OTHER;
        }
        return items.stream()
                .filter(item -> !item.hasId(itemId))
                .flatMap(item -> item.subcategory().stream())
                .filter(subcategory::isSameAs)
                .findFirst()
                .orElse(subcategory);
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

    /** The items in display order, the topmost first (UK-16, UK-17). */
    public List<BudgetItem> items() {
        return Collections.unmodifiableList(items);
    }

    /** The order the user arranged the groups of a section in, {@link SubcategoryOrder#NONE} if never (UK-14). */
    public SubcategoryOrder subcategoryOrderOf(ItemSection section) {
        return subcategoryOrders.getOrDefault(section, SubcategoryOrder.NONE);
    }

    /** The arranged orders of all sections the user arranged the groups of. */
    public Map<ItemSection, SubcategoryOrder> subcategoryOrders() {
        return Map.copyOf(subcategoryOrders);
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
