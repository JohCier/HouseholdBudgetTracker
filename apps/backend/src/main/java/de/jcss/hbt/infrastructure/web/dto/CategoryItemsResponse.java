package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.OverviewCategoryItems;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** The expenses of one category grouped by subcategory, and their sum without unplanned income (UK-10, UK-12). */
public record CategoryItemsResponse(
        ExpenseCategoryDto category,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "11350.00") BigDecimal total,
        List<ItemGroupResponse> groups
) {

    public static CategoryItemsResponse from(OverviewCategoryItems category) {
        return new CategoryItemsResponse(
                ExpenseCategoryDto.from(category.category()),
                category.total().value(),
                category.groups().stream().map(ItemGroupResponse::from).toList()
        );
    }
}
