package com.example.householdbudgettracker.adapter.in.web.dto;

import com.example.householdbudgettracker.domain.BudgetItemId;

import java.util.UUID;

public record BudgetItemIdResponse(UUID id) {

    public static BudgetItemIdResponse from(BudgetItemId id) {
        return new BudgetItemIdResponse(id.value());
    }
}
