package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.List;
import java.util.Objects;

/**
 * Put the subcategory groups of one section of a household plan of the given owner in the
 * given order (UK-14). Whether they are exactly the groups of the section is decided by the plan.
 *
 * @param order the groups of the section, the topmost first
 */
@Command
public record ReorderSubcategoriesCommand(OwnerId owner, HouseholdPlanId planId, ItemSection section,
                                          List<BudgetItemSubcategory> order) {

    public ReorderSubcategoriesCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(section, "section must not be null");
        order = List.copyOf(order);
    }
}
