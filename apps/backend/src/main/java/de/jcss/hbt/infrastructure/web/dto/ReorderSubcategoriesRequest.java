package de.jcss.hbt.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The new order of the subcategory groups of one section: the incomes, or the expenses of one category. */
public record ReorderSubcategoriesRequest(
        @NotNull ItemTypeDto type,
        @Schema(description = "Required for the expenses, must be absent for the incomes") ExpenseCategoryDto category,
        @NotEmpty @Schema(description = "All subcategories of the section, each once, the topmost first; \"Sonstiges\" for the items without one")
        List<@NotBlank String> subcategories
) {
}
