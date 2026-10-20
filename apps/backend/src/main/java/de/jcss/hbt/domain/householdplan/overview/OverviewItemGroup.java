package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.List;

/**
 * Part of the {@link HouseholdPlanOverview} read model: the items of one subcategory group of a
 * section, in display order, and their sum per year and per month (UK-11, UK-12, MJ-9).
 *
 * @param subcategory the name of the group; {@link BudgetItemSubcategory#OTHER} for the items without one
 */
@QueryModel
public record OverviewItemGroup(BudgetItemSubcategory subcategory, Money total, List<OverviewItem> items) {

    public OverviewItemGroup {
        items = List.copyOf(items);
    }

    /** The sum per month: the yearly sum divided by 12 (MJ-9). */
    public Money monthlyTotal() {
        return total.perMonth();
    }
}
