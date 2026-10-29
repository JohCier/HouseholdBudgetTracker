package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.networth.overview.NetWorthHistoryEntry;
import de.jcss.hbt.domain.networth.overview.NetWorthTotals;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record NetWorthHistoryEntryResponse(
        @Schema(example = "2026") int year,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "21700.00") BigDecimal totalAssets,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "7800.00") BigDecimal totalLiabilities,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "13900.00") BigDecimal netWorth,
        boolean overindebted,
        @Schema(nullable = true, description = "null if the previous year has no assets or liabilities recorded")
        NetWorthChangeResponse change
) {

    public static NetWorthHistoryEntryResponse from(NetWorthHistoryEntry entry) {
        NetWorthTotals totals = entry.totals();
        return new NetWorthHistoryEntryResponse(
                totals.year().getValue(),
                totals.totalAssets().value(),
                totals.totalLiabilities().value(),
                totals.netWorth().value(),
                totals.overindebted(),
                NetWorthChangeResponse.from(entry.change()));
    }
}
