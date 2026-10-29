package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.networth.overview.NetWorthChange;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** How the net worth differs from that of the year before (VS-12, VS-13). */
public record NetWorthChangeResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "3400.00",
                description = "Positive if the net worth grew") BigDecimal amount,
        @Schema(example = "32.38", nullable = true,
                description = "Change in percent of the amount of the previous net worth; null if that was zero")
        BigDecimal percentage
) {

    public static NetWorthChangeResponse from(NetWorthChange change) {
        if (change == null) {
            return null;
        }
        return new NetWorthChangeResponse(change.amount().value(),
                change.percentage() == null ? null : change.percentage().value());
    }
}
