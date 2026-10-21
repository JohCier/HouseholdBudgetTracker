package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.List;

/**
 * Part of the {@link HouseholdPlanOverview} read model: the items of one subcategory group of a
 * section, in display order, and their sum per year and per month (UK-11, UK-12, MJ-9).
 *
 * @param subcategory         the name of the group; {@link BudgetItemSubcategory#OTHER} for the items without one
 * @param percentageOfSection how much of the section the group makes up per month, as the pie of a
 *                            category shows it (TK-6, TK-8)
 */
@QueryModel
public record OverviewItemGroup(BudgetItemSubcategory subcategory, Money total, Percentage percentageOfSection,
                                List<OverviewItem> items) {

    public OverviewItemGroup {
        items = List.copyOf(items);
    }

    /** The sum per month: the yearly sum divided by 12 (MJ-9). */
    public Money monthlyTotal() {
        return total.perMonth();
    }

    /** How much of the group's monthly total the monthly amount of one of its items makes up (TK-6, TK-8). */
    public Percentage percentageOf(OverviewItem item) {
        return Percentage.shareOf(item.amount().monthly(), monthlyTotal());
    }
}
