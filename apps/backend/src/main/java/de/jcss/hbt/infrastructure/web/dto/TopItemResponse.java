package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.TopItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of a top 5 list: an expense with the amount the list is ordered by (TP-1 to TP-3, TP-8, TP-9). */
public record TopItemResponse(
        UUID id,
        @Schema(example = "Streaming") String description,
        @Schema(example = "Abonnements", description = "\"Sonstiges\" for the expenses without a subcategory")
        String subcategory,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "5.00",
                description = "What the list is ordered by: the saving potential or the monthly amount of the expense")
        BigDecimal monthlyAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "60.00",
                description = "The yearly value of the monthly amount")
        BigDecimal amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "15.00",
                description = "The monthly amount of the expense itself")
        BigDecimal itemMonthlyAmount
) {

    public static TopItemResponse from(TopItem row) {
        return new TopItemResponse(row.item().id().value(), row.item().description().value(),
                row.item().group().value(), row.monthly().value(), row.annual().value(),
                row.item().amount().monthly().value());
    }
}
