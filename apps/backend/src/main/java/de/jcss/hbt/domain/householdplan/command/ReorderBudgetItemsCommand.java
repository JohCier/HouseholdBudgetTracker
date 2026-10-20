package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.List;
import java.util.Objects;

/**
 * Put the items of one subcategory group of a household plan of the given owner in the given
 * order (UK-14). Whether the items form exactly one group is decided by the plan.
 *
 * @param order the items of the group, the topmost first
 */
@Command
public record ReorderBudgetItemsCommand(OwnerId owner, HouseholdPlanId planId, List<BudgetItemId> order) {

    public ReorderBudgetItemsCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        order = List.copyOf(order);
    }
}
