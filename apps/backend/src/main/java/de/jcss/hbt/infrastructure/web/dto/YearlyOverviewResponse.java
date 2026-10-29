package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * All years of the user with an active plan or one-time items, the newest year first (JU-1, EP-12), and how many
 * deleted plans they have (GP-21).
 */
public record YearlyOverviewResponse(
        List<YearlyOverviewEntryResponse> years,
        @Schema(example = "2") int deletedPlanCount
) {

    public static YearlyOverviewResponse from(YearlyOverview overview) {
        return new YearlyOverviewResponse(overview.years().stream().map(YearlyOverviewEntryResponse::from).toList(),
                overview.deletedPlanCount());
    }
}
