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

/** New values of an item; a missing or blank {@code note} removes the note; {@code category} is required for an expense and must be absent for an income. */
public record ChangeBudgetItemRequest(
        @NotBlank @Size(max = BudgetItemDescription.MAX_LENGTH) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "900.00") BigDecimal amount,
        @Schema(description = "Required for an expense, must be absent for an income") ExpenseCategoryDto category,
        @Size(max = BudgetItemNote.MAX_LENGTH)
        @Schema(nullable = true, description = "Optional; empty or blank means no note") String note
) {
}
