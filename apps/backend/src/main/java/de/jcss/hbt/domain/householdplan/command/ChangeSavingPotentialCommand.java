package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Set, change or remove the saving potential of an expense of a household plan of the given owner
 * (PM-2, PM-4b). Whether the position is an expense and the saving potential fits its amount is
 * decided by the plan.
 *
 * @param savingPotential {@code null} to remove it
 */
@Command
public record ChangeSavingPotentialCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemId itemId,
                                           SavingPotential savingPotential) {

    public ChangeSavingPotentialCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
