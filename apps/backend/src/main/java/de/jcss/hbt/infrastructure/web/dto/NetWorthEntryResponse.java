package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.networth.overview.NetWorthOverviewEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record NetWorthEntryResponse(
        UUID id,
        NetWorthEntryTypeDto type,
        @Schema(example = "Girokonto") String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "3200.00") BigDecimal amount
) {

    public static NetWorthEntryResponse from(NetWorthOverviewEntry entry) {
        return new NetWorthEntryResponse(entry.id().value(), NetWorthEntryTypeDto.from(entry.type()),
                entry.name().value(), entry.amount().value());
    }
}
