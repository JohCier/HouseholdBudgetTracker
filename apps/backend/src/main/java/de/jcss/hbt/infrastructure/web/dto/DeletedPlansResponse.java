package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.overview.DeletedPlans;

import java.util.List;

/** The deleted plans of the user, the newest year first, within a year the latest deleted first. */
public record DeletedPlansResponse(List<DeletedPlanResponse> plans) {

    public static DeletedPlansResponse from(DeletedPlans deleted) {
        return new DeletedPlansResponse(deleted.plans().stream().map(DeletedPlanResponse::from).toList());
    }
}
