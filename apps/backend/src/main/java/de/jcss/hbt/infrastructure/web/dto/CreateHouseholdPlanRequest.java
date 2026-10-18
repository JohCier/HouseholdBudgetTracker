package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateHouseholdPlanRequest(
        @NotNull @Min(HouseholdPlan.FIRST_YEAR) @Max(HouseholdPlan.LAST_YEAR) @Schema(example = "2026") Integer year
) {
}
