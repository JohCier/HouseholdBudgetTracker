package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

/**
 * Part of the {@link HouseholdPlanOverview} read model: a single position of the plan, as
 * shown in the item list.
 *
 * @param category only set for expenses, otherwise {@code null}
 */
@QueryModel
public record OverviewItem(
        BudgetItemId id,
        BudgetItemDescription description,
        Money amount,
        ItemType type,
        ExpenseCategory category
) {
}
