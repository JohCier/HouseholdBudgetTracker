package de.jcss.hbt.adapter.in.web.dto;

import de.jcss.hbt.domain.BudgetItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddIncomeRequest(
        @NotBlank @Size(max = BudgetItem.MAX_DESCRIPTION_LENGTH) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "2500.00") BigDecimal amount,
        @NotNull LocalDate bookingDate
) {
}
