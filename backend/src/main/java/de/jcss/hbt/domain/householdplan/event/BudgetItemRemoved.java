package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/** A position was removed from a household plan. */
public record BudgetItemRemoved(HouseholdPlanId planId, BudgetItemId itemId, Instant occurredAt) implements DomainEvent {

    public BudgetItemRemoved {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
