package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.application.householdplan.CreatedHouseholdPlan;

import java.util.UUID;

/** The id of the new plan and its overview (AU-1, AU-3). */
public record CreatedHouseholdPlanResponse(UUID id, HouseholdPlanOverviewResponse overview) {

    public static CreatedHouseholdPlanResponse from(CreatedHouseholdPlan created) {
        return new CreatedHouseholdPlanResponse(created.id().value(),
                HouseholdPlanOverviewResponse.from(created.overview()));
    }
}
