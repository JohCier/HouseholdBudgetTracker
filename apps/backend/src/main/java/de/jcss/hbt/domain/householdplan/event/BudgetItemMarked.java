package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMark;

import java.time.Instant;
import java.util.Objects;

/**
 * A position of a household plan was marked as important or got a saving potential, or lost it again
 * (PM-7); a changed saving potential counts as set.
 *
 * @param marked whether the mark was set ({@code true}) or removed ({@code false})
 */
public record BudgetItemMarked(HouseholdPlanId planId, BudgetItemId itemId, ItemMark mark, boolean marked,
                               Instant timestamp) implements HouseholdPlanEvent {

    public BudgetItemMarked {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(mark, "mark must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
