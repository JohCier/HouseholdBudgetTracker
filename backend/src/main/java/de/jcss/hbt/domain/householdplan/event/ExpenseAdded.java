package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainEvent;

/** An expense position was added to a household plan. */
public record ExpenseAdded(HouseholdPlanId planId, BudgetItemId itemId) implements DomainEvent {
}
