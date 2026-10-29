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
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "208.33", description = "The yearly total divided by 12")
        BigDecimal monthlyTotalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "150.00", description = "The yearly total divided by 12")
        BigDecimal monthlyTotalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "58.33", description = "The yearly balance divided by 12")
        BigDecimal monthlyBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "35.00",
                description = "The sum of the monthly saving potentials of all expenses")
        BigDecimal monthlySavingPotential,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "420.00",
                description = "Twelve times the monthly saving potential")
        BigDecimal savingPotential,
        @Schema(description = "The subcategories with a saving potential, the largest first (SU-3, SU-4)")
        List<SubcategorySavingPotentialResponse> savingPotentialsBySubcategory,
        @Schema(description = "At most five expenses with the highest saving potential, the largest first (TP-1, TP-4)")
        List<TopItemResponse> topSavingPotentials,
        @Schema(description = "At most five expenses of living costs and leisure with the highest monthly amount, "
                + "the largest first (TP-2, TP-4)")
        List<TopItemResponse> topExpenses,
        boolean expensesExceedIncome,
        List<CategoryBreakdownResponse> categoryBreakdowns,
        @Schema(description = "In display order, the topmost first") List<BudgetItemResponse> items,
        @Schema(description = "The incomes grouped by subcategory, in display order") List<ItemGroupResponse> incomeGroups,
        @Schema(description = "The categories that have expenses, in the order of the 50/30/20 rule")
        List<CategoryItemsResponse> expenseCategories
) {

    public static HouseholdPlanOverviewResponse from(HouseholdPlanOverview overview) {
        return new HouseholdPlanOverviewResponse(
                overview.planId().value(),
                overview.year().getValue(),
                overview.totalIncome().value(),
                overview.totalExpense().value(),
                overview.balance().value(),
                overview.monthlyTotalIncome().value(),
                overview.monthlyTotalExpense().value(),
                overview.monthlyBalance().value(),
                overview.monthlySavingPotential().value(),
                overview.savingPotential().value(),
                overview.savingPotentialsBySubcategory().stream().map(SubcategorySavingPotentialResponse::from).toList(),
                overview.topSavingPotentials().stream().map(TopItemResponse::from).toList(),
                overview.topExpenses().stream().map(TopItemResponse::from).toList(),
                overview.expensesExceedIncome(),
                overview.categoryBreakdowns().stream().map(CategoryBreakdownResponse::from).toList(),
                overview.items().stream().map(BudgetItemResponse::from).toList(),
                overview.incomeGroups().stream().map(ItemGroupResponse::from).toList(),
                overview.expenseCategories().stream().map(CategoryItemsResponse::from).toList()
        );
    }
}
