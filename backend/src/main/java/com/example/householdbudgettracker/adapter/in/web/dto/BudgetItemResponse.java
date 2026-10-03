package com.example.householdbudgettracker.adapter.in.web.dto;

import com.example.householdbudgettracker.domain.BudgetItem;
import com.example.householdbudgettracker.domain.ExpenseCategory;
import com.example.householdbudgettracker.domain.ItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetItemResponse(
        UUID id,
        String description,
        BigDecimal amount,
        ItemType type,
        ExpenseCategory category
) {

    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(
                item.id().value(),
                item.description(),
                item.amount().value(),
                item.type(),
                item.category().orElse(null)
        );
    }
}
