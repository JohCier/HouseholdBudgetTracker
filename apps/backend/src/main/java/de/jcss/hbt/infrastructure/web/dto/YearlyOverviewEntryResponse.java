package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverviewEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record YearlyOverviewEntryResponse(
        UUID planId,
        @Schema(example = "2026") int year,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "30900.00") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "24250.00") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "6650.00") BigDecimal balance,
        boolean expensesExceedIncome,
        @Schema(example = "3.00", nullable = true,
                description = "Change from the previous year in percent; null without a plan of the previous year or if its income was zero")
        BigDecimal incomeChange,
        @Schema(example = "-3.00", nullable = true,
                description = "Change from the previous year in percent; null without a plan of the previous year or if its expenses were zero")
        BigDecimal expenseChange,
        @Schema(example = "33.00", nullable = true,
                description = "Change from the previous year in percent, positive if the balance grew; null without a plan of the previous year or if its balance was zero")
        BigDecimal balanceChange
) {

    public static YearlyOverviewEntryResponse from(YearlyOverviewEntry entry) {
        return new YearlyOverviewEntryResponse(
                entry.planId().value(),
                entry.year().getValue(),
                entry.totalIncome().value(),
                entry.totalExpense().value(),
                entry.balance().value(),
                entry.expensesExceedIncome(),
                valueOf(entry.incomeChange()),
                valueOf(entry.expenseChange()),
                valueOf(entry.balanceChange())
        );
    }

    private static BigDecimal valueOf(Percentage change) {
        return change == null ? null : change.value();
    }
}
