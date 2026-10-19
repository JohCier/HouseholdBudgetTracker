package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.application.householdplan.AddedBudgetItem;

import java.util.UUID;

/** The id of the new item and the overview of its plan after adding it (AU-1, AU-3). */
public record AddedBudgetItemResponse(UUID id, HouseholdPlanOverviewResponse overview) {

    public static AddedBudgetItemResponse from(AddedBudgetItem added) {
        return new AddedBudgetItemResponse(added.id().value(), HouseholdPlanOverviewResponse.from(added.overview()));
    }
}
