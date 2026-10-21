package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

/**
 * Part of the {@link HouseholdPlanOverview} read model: the saving potential of all expenses of one
 * subcategory, across the categories of the 50/30/20 rule (SU-1, SU-2).
 *
 * @param subcategory the name as the entries show it first; {@link BudgetItemSubcategory#OTHER} for the
 *                    expenses without one
 * @param monthly     the sum of the monthly saving potentials of its expenses
 */
@QueryModel
public record SubcategorySavingPotential(BudgetItemSubcategory subcategory, Money monthly) {

    /** The saving potential for the whole year: twelve times the monthly one (SU-1). */
    public Money annual() {
        return monthly.timesMonthsPerYear();
    }
}
