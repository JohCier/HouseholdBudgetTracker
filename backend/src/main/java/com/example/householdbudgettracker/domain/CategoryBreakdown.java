package com.example.householdbudgettracker.domain;

import java.math.BigDecimal;

/**
 * Value object summarizing, for a single {@link ExpenseCategory}, the actual spend and
 * how it compares to the target share of income defined by the 50/30/20 rule.
 */
public record CategoryBreakdown(
        ExpenseCategory category,
        Money total,
        BigDecimal actualPercentageOfIncome,
        int targetPercentageOfIncome,
        boolean withinTarget
) {
}
