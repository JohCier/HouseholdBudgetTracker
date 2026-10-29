package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeOverviewItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OneTimeItemResponse(
        UUID id,
        ItemTypeDto type,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1200.00") BigDecimal amount,
        @Schema(example = "2026-03-15") LocalDate date,
        @Schema(nullable = true, description = "Only set if the item has a note")
        String note
) {

    public static OneTimeItemResponse from(OneTimeOverviewItem item) {
        return new OneTimeItemResponse(
                item.id().value(),
                ItemTypeDto.from(item.type()),
                item.description().value(),
                item.amount().value(),
                item.date(),
                item.note() == null ? null : item.note().value());
    }
}
