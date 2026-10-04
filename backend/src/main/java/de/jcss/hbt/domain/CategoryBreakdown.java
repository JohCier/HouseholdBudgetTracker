package de.jcss.hbt.domain;

import java.math.BigDecimal;

/**
 * Value object summarizing, for a single {@link ExpenseCategory}, the actual spend and
 * whether it meets the target share of income defined by the 50/30/20 rule.
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
