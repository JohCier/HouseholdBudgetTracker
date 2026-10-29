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
import java.time.LocalDate;

/** A one-time item as entered, to create or to change one (EP-1, EP-7). */
public record OneTimeItemRequest(
        @NotNull ItemTypeDto type,
        @NotBlank @Size(max = BudgetItemDescription.MAX_LENGTH) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "1200.00") BigDecimal amount,
        @NotNull @Schema(example = "2026-03-15", description = "The day of the item, 1900-01-01 to 2200-12-31")
        LocalDate date,
        @Size(max = BudgetItemNote.MAX_LENGTH)
        @Schema(nullable = true, description = "Optional; empty or blank means no note") String note
) {
}
