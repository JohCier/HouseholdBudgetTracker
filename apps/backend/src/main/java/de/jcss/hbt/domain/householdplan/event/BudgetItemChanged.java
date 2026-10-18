package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;

import java.time.Instant;
import java.util.Objects;

/** The description, amount or category of a position of a household plan was changed. */
public record BudgetItemChanged(HouseholdPlanId planId, BudgetItemId itemId, Instant timestamp) implements HouseholdPlanEvent {

    public BudgetItemChanged {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
