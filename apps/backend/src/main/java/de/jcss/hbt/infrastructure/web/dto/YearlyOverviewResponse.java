package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;

import java.util.List;

/** All plans of the user, the newest year first. */
public record YearlyOverviewResponse(List<YearlyOverviewEntryResponse> years) {

    public static YearlyOverviewResponse from(YearlyOverview overview) {
        return new YearlyOverviewResponse(overview.years().stream().map(YearlyOverviewEntryResponse::from).toList());
    }
}
