package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.application.householdplan.CopiedHouseholdPlan;

import java.util.UUID;

/** The id of the plan of the target year and its overview after the copy (PU-15, AU-1). */
public record CopiedHouseholdPlanResponse(UUID id, HouseholdPlanOverviewResponse overview) {

    public static CopiedHouseholdPlanResponse from(CopiedHouseholdPlan copied) {
        return new CopiedHouseholdPlanResponse(copied.id().value(), HouseholdPlanOverviewResponse.from(copied.overview()));
    }
}
