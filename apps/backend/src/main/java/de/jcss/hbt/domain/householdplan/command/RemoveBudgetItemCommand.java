package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.util.Objects;

/** Remove an income or expense position from a household plan of the given owner. */
public record RemoveBudgetItemCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemId itemId)
        implements DomainCommand {

    public RemoveBudgetItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
