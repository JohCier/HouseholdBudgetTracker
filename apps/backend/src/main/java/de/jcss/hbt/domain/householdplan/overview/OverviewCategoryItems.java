package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.List;

/**
 * Part of the {@link HouseholdPlanOverview} read model: the expenses of one category grouped by
 * subcategory, and their sum (UK-10, UK-12). Unlike {@link CategoryBreakdown#total()}, the sum
 * holds only the recorded expenses, not the unplanned income.
 */
@QueryModel
public record OverviewCategoryItems(ExpenseCategory category, Money total, List<OverviewItemGroup> groups) {

    public OverviewCategoryItems {
        groups = List.copyOf(groups);
    }
}
