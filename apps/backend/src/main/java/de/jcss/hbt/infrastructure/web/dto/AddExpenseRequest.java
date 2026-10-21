package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
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
        @NotNull @Schema(description = "Whether the amount is a monthly or a yearly amount") AmountPeriodDto period,
        @NotNull ExpenseCategoryDto category,
        @Size(max = BudgetItemSubcategory.MAX_LENGTH)
        @Schema(nullable = true, description = "Optional; empty or blank means none, i.e. \"Sonstiges\"") String subcategory,
        @Size(max = BudgetItemNote.MAX_LENGTH)
        @Schema(nullable = true, description = "Optional; empty or blank means no note") String note,
        @Schema(nullable = true, description = "Optional, only to restore a removed item; a new expense is not important")
        Boolean important,
        @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "5.00", nullable = true,
                description = "Optional, only to restore a removed item: the monthly saving potential, at most the monthly amount")
        BigDecimal savingPotential
) {
}
