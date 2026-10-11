package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.BudgetItemId;

import java.util.UUID;

public record BudgetItemIdResponse(UUID id) {

    public static BudgetItemIdResponse from(BudgetItemId id) {
        return new BudgetItemIdResponse(id.value());
    }
}
