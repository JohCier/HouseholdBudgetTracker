package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** The one-time items of a year by date, with their totals (EP-8, EP-9). */
public record OneTimeItemOverviewResponse(
        @Schema(example = "2026") int year,
        List<OneTimeItemResponse> items,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "800.00") BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "1200.00") BigDecimal totalExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "-400.00") BigDecimal balance,
        boolean expensesExceedIncome
) {

    public static OneTimeItemOverviewResponse from(OneTimeItemOverview overview) {
        return new OneTimeItemOverviewResponse(
                overview.year().getValue(),
                overview.items().stream().map(OneTimeItemResponse::from).toList(),
                overview.totalIncome().value(),
                overview.totalExpense().value(),
                overview.balance().value(),
                overview.expensesExceedIncome());
    }
}
