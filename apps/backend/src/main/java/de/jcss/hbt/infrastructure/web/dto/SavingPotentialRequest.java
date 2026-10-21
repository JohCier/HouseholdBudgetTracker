package de.jcss.hbt.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** The saving potential of an expense, always per month; at most the monthly amount of the expense. */
public record SavingPotentialRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "5.00") BigDecimal monthlyAmount
) {
}
