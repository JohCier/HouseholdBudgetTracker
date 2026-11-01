package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finding, grouping and ordering the items of a {@link HouseholdPlan} by subcategory (UK-5, UK-14). Only the
 * aggregate root calls it, on its own list of items; it holds no state and records no events, which stays with the
 * root. Not a DDD building block of its own, only the part of the root that would otherwise make it too long.
 */
final class ItemGroups {

    private ItemGroups() {
    }

    /** The position of the item in the list. */
    static int indexOf(List<BudgetItem> items, BudgetItemId itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).hasId(itemId)) {
                return i;
            }
        }
        throw new BudgetItemNotFoundException(itemId);
    }

    /**
     * Puts the items of one subcategory group in the given order (UK-14); the other items keep their positions.
     *
     * @param order all items of one group, each once, the topmost first
     * @return whether the order changed
     * @throws BudgetItemNotFoundException if an item is not in the list
     * @throws DomainValidationException   if the items are not exactly those of one group
     */
    static boolean reorder(List<BudgetItem> items, List<BudgetItemId> order) {
        Objects.requireNonNull(order, "order must not be null");
        if (order.isEmpty()) {
            throw new DomainValidationException("the order must name the items of a group");
        }
        BudgetItem first = items.get(indexOf(items, order.getFirst()));
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isInSameGroupAs(first)) {
                positions.add(i);
            }
        }
        List<BudgetItem> reordered = new ArrayList<>();
        for (BudgetItemId itemId : order) {
            BudgetItem item = items.get(indexOf(items, itemId));
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
        return changed;
    }

    /** The subcategory groups the section has, each once, in the spelling of its items. */
    static List<BudgetItemSubcategory> of(List<BudgetItem> items, ItemSection section) {
        List<BudgetItemSubcategory> groups = new ArrayList<>();
        for (BudgetItem item : items) {
            if (item.section().equals(section) && groups.stream().noneMatch(item.group()::isSameAs)) {
                groups.add(item.group());
            }
        }
        return groups;
    }

    /**
     * The spelling an item gets for the given subcategory: that of another item with the same subcategory if there is
     * one, so a group is spelled alike (UK-5); "Sonstiges" always as {@link BudgetItemSubcategory#OTHER}.
     *
     * @param itemId the item the subcategory is for, which does not count as another; {@code null} for a new one
     */
    static BudgetItemSubcategory spellingOf(List<BudgetItem> items, BudgetItemSubcategory subcategory,
                                            BudgetItemId itemId) {
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
}
