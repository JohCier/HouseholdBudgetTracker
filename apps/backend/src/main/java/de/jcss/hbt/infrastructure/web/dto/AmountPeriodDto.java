package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.valueobject.AmountPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

/** API representation of {@link AmountPeriod}. */
@Schema(name = "AmountPeriod", enumAsRef = true)
public enum AmountPeriodDto {
    MONTHLY,
    YEARLY;

    public static AmountPeriodDto from(AmountPeriod period) {
        return switch (period) {
            case MONTHLY -> MONTHLY;
            case YEARLY -> YEARLY;
        };
    }

    public AmountPeriod toDomain() {
        return switch (this) {
            case MONTHLY -> AmountPeriod.MONTHLY;
            case YEARLY -> AmountPeriod.YEARLY;
        };
    }
}
