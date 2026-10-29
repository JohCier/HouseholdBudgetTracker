package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverviewEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record YearlyOverviewEntryResponse(
        @Schema(nullable = true, description = "null for a year with one-time items but without a plan (EP-12)")
        UUID planId,
        @Schema(example = "2026") int year,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "30900.00", nullable = true,
                description = "null without a plan (EP-12)") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "24250.00", nullable = true,
                description = "null without a plan (EP-12)") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "6650.00", nullable = true,
                description = "null without a plan (EP-12)") BigDecimal balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "2575.00", description = "The yearly total divided by 12; null without a plan", nullable = true)
        BigDecimal monthlyTotalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "2020.83", description = "The yearly total divided by 12; null without a plan", nullable = true)
        BigDecimal monthlyTotalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "554.17", description = "The yearly balance divided by 12; null without a plan", nullable = true)
        BigDecimal monthlyBalance,
        boolean expensesExceedIncome,
        @Schema(example = "3.00", nullable = true,
                description = "Change from the previous year in percent; null without a plan of the previous year or if its income was zero")
        BigDecimal incomeChange,
        @Schema(example = "-3.00", nullable = true,
                description = "Change from the previous year in percent; null without a plan of the previous year or if its expenses were zero")
        BigDecimal expenseChange,
        @Schema(example = "33.00", nullable = true,
                description = "Change from the previous year in percent, positive if the balance grew; null without a plan of the previous year or if its balance was zero")
        BigDecimal balanceChange,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "800.00",
                description = "Sum of the one-time incomes of the year, always a yearly amount (EP-13, EP-14)")
        BigDecimal oneTimeIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1200.00",
                description = "Sum of the one-time expenses of the year, always a yearly amount (EP-13, EP-14)")
        BigDecimal oneTimeExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "-400.00",
                description = "One-time incomes minus one-time expenses of the year (EP-13)")
        BigDecimal oneTimeBalance,
        boolean oneTimeExpensesExceedIncome
) {

    public static YearlyOverviewEntryResponse from(YearlyOverviewEntry entry) {
        return new YearlyOverviewEntryResponse(
                entry.hasPlan() ? entry.planId().value() : null,
                entry.year().getValue(),
                valueOf(entry.totalIncome()),
                valueOf(entry.totalExpense()),
                valueOf(entry.balance()),
                valueOf(entry.monthlyTotalIncome()),
                valueOf(entry.monthlyTotalExpense()),
                valueOf(entry.monthlyBalance()),
                entry.expensesExceedIncome(),
                valueOf(entry.incomeChange()),
                valueOf(entry.expenseChange()),
                valueOf(entry.balanceChange()),
                entry.oneTimeIncome().value(),
                entry.oneTimeExpense().value(),
                entry.oneTimeBalance().value(),
                entry.oneTimeExpensesExceedIncome()
        );
    }

    private static BigDecimal valueOf(Money amount) {
        return amount == null ? null : amount.value();
    }

    private static BigDecimal valueOf(Percentage change) {
        return change == null ? null : change.value();
    }
}
