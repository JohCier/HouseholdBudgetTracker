package de.jcss.hbt.domain.householdplan.event;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/** An income position was added to a household plan. */
public record IncomeAdded(HouseholdPlanId planId, BudgetItemId itemId, Instant timestamp) implements DomainEvent {

    public IncomeAdded {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
