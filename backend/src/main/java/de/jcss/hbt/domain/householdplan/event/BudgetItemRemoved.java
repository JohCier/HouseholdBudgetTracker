package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainEvent;

/** A position was removed from a household plan. */
public record BudgetItemRemoved(HouseholdPlanId planId, BudgetItemId itemId) implements DomainEvent {
}
