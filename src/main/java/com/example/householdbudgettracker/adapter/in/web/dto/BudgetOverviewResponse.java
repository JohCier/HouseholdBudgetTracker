package com.example.householdbudgettracker.adapter.in.web.dto;

import com.example.householdbudgettracker.domain.BudgetOverview;

import java.math.BigDecimal;
import java.util.List;

public record BudgetOverviewResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdownResponse> categoryBreakdowns
) {

    public static BudgetOverviewResponse from(BudgetOverview overview) {
        return new BudgetOverviewResponse(
                overview.totalIncome().value(),
                overview.totalExpense().value(),
                overview.balance().value(),
                overview.expensesExceedIncome(),
                overview.categoryBreakdowns().stream().map(CategoryBreakdownResponse::from).toList()
        );
    }
}
