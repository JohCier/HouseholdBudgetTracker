package de.jcss.hbt.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.BudgetOverview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record BudgetOverviewResponse(
        @Schema(example = "2026") int year,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "2500.00") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1800.00") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "700.00") BigDecimal balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdownResponse> categoryBreakdowns
) {

    public static BudgetOverviewResponse from(BudgetOverview overview) {
        return new BudgetOverviewResponse(
                overview.year().getValue(),
                overview.totalIncome().value(),
                overview.totalExpense().value(),
                overview.balance().value(),
                overview.expensesExceedIncome(),
                overview.categoryBreakdowns().stream().map(CategoryBreakdownResponse::from).toList()
        );
    }
}
