package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;

import java.util.UUID;

public record HouseholdPlanIdResponse(UUID id) {

    public static HouseholdPlanIdResponse from(HouseholdPlanId id) {
        return new HouseholdPlanIdResponse(id.value());
    }
}
