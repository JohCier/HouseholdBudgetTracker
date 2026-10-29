package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.networth.overview.NetWorthOverview;
import de.jcss.hbt.domain.networth.overview.NetWorthTotals;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** The assets and liabilities of a year, each in the order they were recorded, and their sums (VS-6, VS-8). */
public record NetWorthOverviewResponse(
        @Schema(example = "2026") int year,
        List<NetWorthEntryResponse> assets,
        List<NetWorthEntryResponse> liabilities,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "21700.00") BigDecimal totalAssets,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "7800.00") BigDecimal totalLiabilities,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "13900.00",
                description = "Assets minus liabilities; negative if overindebted") BigDecimal netWorth,
        boolean overindebted,
        @Schema(nullable = true, description = "null if this or the previous year has no assets or liabilities recorded")
        NetWorthChangeResponse change
) {

    public static NetWorthOverviewResponse from(NetWorthOverview overview) {
        NetWorthTotals totals = overview.totals();
        return new NetWorthOverviewResponse(
                overview.year().getValue(),
                overview.assets().stream().map(NetWorthEntryResponse::from).toList(),
                overview.liabilities().stream().map(NetWorthEntryResponse::from).toList(),
                totals.totalAssets().value(),
                totals.totalLiabilities().value(),
                totals.netWorth().value(),
                totals.overindebted(),
                NetWorthChangeResponse.from(overview.change()));
    }
}
