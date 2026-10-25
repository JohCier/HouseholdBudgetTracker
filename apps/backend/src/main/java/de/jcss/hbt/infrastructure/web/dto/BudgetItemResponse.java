package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.valueobject.Percentage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetItemResponse(
        UUID id,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "850.00",
                description = "As entered, a monthly or a yearly amount depending on period") BigDecimal amount,
        AmountPeriodDto period,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "850.00") BigDecimal monthlyAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "10200.00") BigDecimal annualAmount,
        ItemTypeDto type,
        @Schema(nullable = true, description = "Only set for expenses")
        ExpenseCategoryDto category,
        @Schema(nullable = true, description = "Only set if the item has a subcategory; otherwise it is listed under \"Sonstiges\"")
        String subcategory,
        @Schema(nullable = true, description = "Only set if the item has a note")
        String note,
        boolean important,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "5.00", nullable = true,
                description = "The monthly saving potential; only set for expenses that have one")
        BigDecimal savingPotential,
        @Schema(nullable = true, example = "80.00",
                description = "Only set for the items of a group: how much of the group the item makes up per month, in percent (TK-8)")
        BigDecimal percentageOfGroup
) {

    public static BudgetItemResponse from(OverviewItem item) {
        return from(item, null);
    }

    /** An item of a group with its share of the group per month (TK-8). */
    public static BudgetItemResponse from(OverviewItem item, Percentage percentageOfGroup) {
        return new BudgetItemResponse(
                item.id().value(),
                item.description().value(),
                item.amount().value().value(),
                AmountPeriodDto.from(item.amount().period()),
                item.amount().monthly().value(),
                item.amount().annual().value(),
                ItemTypeDto.from(item.type()),
                item.category() == null ? null : ExpenseCategoryDto.from(item.category()),
                item.subcategory() == null ? null : item.subcategory().value(),
                item.note() == null ? null : item.note().value(),
                item.marks().important(),
                item.marks().savingPotential() == null ? null : item.marks().savingPotential().monthly().value(),
                percentageOfGroup == null ? null : percentageOfGroup.value()
        );
    }
}
