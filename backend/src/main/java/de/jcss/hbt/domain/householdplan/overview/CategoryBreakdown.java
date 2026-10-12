package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.householdplan.TargetDirection;
import de.jcss.hbt.domain.shared.ReadModel;

/**
 * Part of the {@link HouseholdPlanOverview} read model summarizing, for a single
 * {@link ExpenseCategory}, the actual spend and whether it meets the target share of
 * income defined by the 50/30/20 rule.
 */
@ReadModel
public record CategoryBreakdown(
        ExpenseCategory category,
        Money total,
        Percentage actualPercentageOfIncome,
        Percentage targetPercentageOfIncome,
        TargetDirection targetDirection,
        boolean targetMet
) {

    /** Takes the figures of the category from the summary, which works them out. */
    public static CategoryBreakdown of(ExpenseCategory category, BudgetSummary summary) {
        return new CategoryBreakdown(category, summary.expensesIn(category), summary.shareOfIncome(category),
                category.targetPercentageOfIncome(), category.targetDirection(), summary.isTargetMet(category));
    }
}
