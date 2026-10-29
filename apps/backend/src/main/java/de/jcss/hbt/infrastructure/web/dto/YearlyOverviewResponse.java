package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;

import java.util.List;

/** All years of the user with a plan or one-time items, the newest year first (JU-1, EP-12). */
public record YearlyOverviewResponse(List<YearlyOverviewEntryResponse> years) {

    public static YearlyOverviewResponse from(YearlyOverview overview) {
        return new YearlyOverviewResponse(overview.years().stream().map(YearlyOverviewEntryResponse::from).toList());
    }
}
