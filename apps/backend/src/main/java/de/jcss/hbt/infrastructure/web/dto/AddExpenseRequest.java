package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddExpenseRequest(
        @NotBlank @Size(max = BudgetItemDescription.MAX_LENGTH) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "850.00") BigDecimal amount,
        @NotNull ExpenseCategoryDto category,
        @Size(max = BudgetItemNote.MAX_LENGTH)
        @Schema(nullable = true, description = "Optional; empty or blank means no note") String note
) {
}
