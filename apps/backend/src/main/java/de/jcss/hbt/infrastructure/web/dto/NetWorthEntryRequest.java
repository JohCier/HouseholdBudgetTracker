package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.networth.NetWorthEntryName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** An asset or a liability to record or the new values of one (VS-1, VS-5). */
public record NetWorthEntryRequest(
        @NotNull NetWorthEntryTypeDto type,
        @NotBlank @Size(max = NetWorthEntryName.MAX_LENGTH) @Schema(example = "Girokonto") String name,
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        @Schema(implementation = String.class, format = "decimal", example = "3200.00",
                description = "Positive for liabilities as well") BigDecimal amount
) {
}
