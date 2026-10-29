package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Mark a position of a household plan of the given owner as important, or no longer (PM-1, PM-4a). */
@Command
public record MarkBudgetItemImportantCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemId itemId,
                                             boolean important) {

    public MarkBudgetItemImportantCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
