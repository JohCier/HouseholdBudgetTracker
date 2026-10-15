package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.householdplan.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;

/** API representation of {@link ItemType}. */
@Schema(name = "ItemType", enumAsRef = true)
public enum ItemTypeDto {
    INCOME,
    EXPENSE;

    public static ItemTypeDto from(ItemType type) {
        return switch (type) {
            case INCOME -> INCOME;
            case EXPENSE -> EXPENSE;
        };
    }
}
