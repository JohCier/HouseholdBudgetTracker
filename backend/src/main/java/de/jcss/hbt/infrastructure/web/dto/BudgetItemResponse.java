package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetItemResponse(
        UUID id,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "850.00") BigDecimal amount,
        ItemTypeDto type,
        @Schema(nullable = true, description = "Only set for expenses")
        ExpenseCategoryDto category
) {

    public static BudgetItemResponse from(OverviewItem item) {
        return new BudgetItemResponse(
                item.id().value(),
                item.description(),
                item.amount().value(),
                ItemTypeDto.from(item.type()),
                item.category() == null ? null : ExpenseCategoryDto.from(item.category())
        );
    }
}
