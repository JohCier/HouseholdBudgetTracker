package de.jcss.hbt.adapter.in.web.dto;

import de.jcss.hbt.domain.BudgetItemId;

import java.util.UUID;

public record BudgetItemIdResponse(UUID id) {

    public static BudgetItemIdResponse from(BudgetItemId id) {
        return new BudgetItemIdResponse(id.value());
    }
}
