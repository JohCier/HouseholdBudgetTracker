package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HouseholdPlanOverviewResponse(
        UUID id,
        @Schema(example = "2026") int year,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "2500.00") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1800.00") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "700.00") BigDecimal balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdownResponse> categoryBreakdowns,
        @Schema(description = "Most recently added first") List<BudgetItemResponse> items
) {

    public static HouseholdPlanOverviewResponse from(HouseholdPlanOverview overview) {
        return new HouseholdPlanOverviewResponse(
                overview.planId().value(),
                overview.year().getValue(),
                overview.totalIncome().value(),
                overview.totalExpense().value(),
                overview.balance().value(),
                overview.expensesExceedIncome(),
                overview.categoryBreakdowns().stream().map(CategoryBreakdownResponse::from).toList(),
                overview.items().stream().map(BudgetItemResponse::from).toList()
        );
    }
}
