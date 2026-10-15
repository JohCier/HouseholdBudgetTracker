package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.CategoryBreakdown;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
        ExpenseCategoryDto category,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "850.00") BigDecimal total,
        @Schema(example = "34.00") BigDecimal actualPercentageOfIncome,
        int targetPercentageOfIncome,
        TargetDirectionDto targetDirection,
        boolean targetMet
) {

    public static CategoryBreakdownResponse from(CategoryBreakdown breakdown) {
        return new CategoryBreakdownResponse(
                ExpenseCategoryDto.from(breakdown.category()),
                breakdown.total().value(),
                breakdown.actualPercentageOfIncome().value(),
                breakdown.targetPercentageOfIncome().value().intValueExact(),
                TargetDirectionDto.from(breakdown.targetDirection()),
                breakdown.targetMet()
        );
    }
}
