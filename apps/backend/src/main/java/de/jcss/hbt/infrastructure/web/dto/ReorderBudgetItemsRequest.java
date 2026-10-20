package de.jcss.hbt.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** The new order of the items of one subcategory group. */
public record ReorderBudgetItemsRequest(
        @NotEmpty @Schema(description = "All items of one group, each once, the topmost first")
        List<@NotNull UUID> itemIds
) {
}
