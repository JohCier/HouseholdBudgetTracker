package com.example.householdbudgettracker.adapter.in.web.dto;

import com.example.householdbudgettracker.domain.CategoryBreakdown;
import com.example.householdbudgettracker.domain.ExpenseCategory;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
        ExpenseCategory category,
        BigDecimal total,
        BigDecimal actualPercentageOfIncome,
        int targetPercentageOfIncome,
        boolean withinTarget
) {

    public static CategoryBreakdownResponse from(CategoryBreakdown breakdown) {
        return new CategoryBreakdownResponse(
                breakdown.category(),
                breakdown.total().value(),
                breakdown.actualPercentageOfIncome(),
                breakdown.targetPercentageOfIncome(),
                breakdown.withinTarget()
        );
    }
}
