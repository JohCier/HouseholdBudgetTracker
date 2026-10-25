package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Remove an income or expense position from a household plan of the given owner. */
@Command
public record RemoveBudgetItemCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemId itemId) {

    public RemoveBudgetItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
