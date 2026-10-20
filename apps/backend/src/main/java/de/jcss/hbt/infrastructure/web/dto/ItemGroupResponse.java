package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.OverviewItemGroup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** The items of one subcategory group, in display order, and their sum (UK-11, UK-12). */
public record ItemGroupResponse(
        @Schema(example = "Auto", description = "\"Sonstiges\" for the items without a subcategory") String subcategory,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1750.00") BigDecimal total,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "145.83", description = "The yearly total divided by 12")
        BigDecimal monthlyTotal,
        List<BudgetItemResponse> items
) {

    public static ItemGroupResponse from(OverviewItemGroup group) {
        return new ItemGroupResponse(
                group.subcategory().value(),
                group.total().value(),
                group.monthlyTotal().value(),
                group.items().stream().map(BudgetItemResponse::from).toList()
        );
    }
}
