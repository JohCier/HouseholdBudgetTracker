package de.jcss.hbt.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateHouseholdPlanRequest(
        @NotNull @Min(1) @Max(9999) @Schema(example = "2026") Integer year
) {
}
