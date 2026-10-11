package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.TargetDirection;

import java.math.BigDecimal;

/**
 * Part of the {@link HouseholdPlanOverview} read model summarizing, for a single
 * {@link ExpenseCategory}, the actual spend and whether it meets the target share of
 * income defined by the 50/30/20 rule.
 */
public record CategoryBreakdown(
        ExpenseCategory category,
        Money total,
        BigDecimal actualPercentageOfIncome,
        int targetPercentageOfIncome,
        TargetDirection targetDirection,
        boolean targetMet
) {
}
