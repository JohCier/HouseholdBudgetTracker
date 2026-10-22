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
        @Schema(example = "13.00",
                description = "How much of the income or of the category total the group makes up per month, in percent (TK-8)")
        BigDecimal percentageOfSection,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "15.00",
                description = "The sum of the monthly saving potentials of its items, \"0.00\" if none has one (SU-10)")
        BigDecimal monthlySavingPotential,
        List<BudgetItemResponse> items
) {

    public static ItemGroupResponse from(OverviewItemGroup group) {
        return new ItemGroupResponse(
                group.subcategory().value(),
                group.total().value(),
                group.monthlyTotal().value(),
                group.percentageOfSection().value(),
                group.monthlySavingPotential().value(),
                group.items().stream().map(item -> BudgetItemResponse.from(item, group.percentageOf(item))).toList()
        );
    }
}
