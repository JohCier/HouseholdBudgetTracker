package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlanEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeletedPlanResponse(
        UUID planId,
        @Schema(example = "2026") int year,
        @Schema(example = "2026-10-29T20:15:00Z") Instant deletedAt,
        @Schema(example = "12") int itemCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "38400.00") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "34200.00") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "4200.00") BigDecimal balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "3200.00", description = "The yearly total divided by 12")
        BigDecimal monthlyTotalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "2850.00", description = "The yearly total divided by 12")
        BigDecimal monthlyTotalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "350.00", description = "The yearly balance divided by 12")
        BigDecimal monthlyBalance,
        boolean expensesExceedIncome
) {

    public static DeletedPlanResponse from(DeletedPlanEntry entry) {
        return new DeletedPlanResponse(
                entry.planId().value(),
                entry.year().getValue(),
                entry.deletedAt(),
                entry.itemCount(),
                entry.totalIncome().value(),
                entry.totalExpense().value(),
                entry.balance().value(),
                entry.monthlyTotalIncome().value(),
                entry.monthlyTotalExpense().value(),
                entry.monthlyBalance().value(),
                entry.expensesExceedIncome()
        );
    }
}
