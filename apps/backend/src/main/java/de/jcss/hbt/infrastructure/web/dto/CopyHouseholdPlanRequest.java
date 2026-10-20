package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param overwrite whether a plan the user already has for the target year may be overwritten (PU-9); {@code false} if missing
 */
public record CopyHouseholdPlanRequest(
        @NotNull @Min(HouseholdPlan.FIRST_YEAR) @Max(HouseholdPlan.LAST_YEAR) @Schema(example = "2027") Integer targetYear,
        Boolean overwrite
) {
}
