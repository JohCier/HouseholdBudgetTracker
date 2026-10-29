package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.networth.NetWorthStatement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param overwrite whether the assets and liabilities the user already recorded for the target year may be overwritten (VS-16); {@code false} if missing
 */
public record CopyNetWorthRequest(
        @NotNull @Min(NetWorthStatement.FIRST_YEAR) @Max(NetWorthStatement.LAST_YEAR) @Schema(example = "2027") Integer targetYear,
        Boolean overwrite
) {
}
