package de.jcss.hbt.infrastructure.web.dto;

import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import io.swagger.v3.oas.annotations.media.Schema;

/** API representation of {@link NetWorthEntryType}. */
@Schema(name = "NetWorthEntryType", enumAsRef = true)
public enum NetWorthEntryTypeDto {
    ASSET,
    LIABILITY;

    public static NetWorthEntryTypeDto from(NetWorthEntryType type) {
        return switch (type) {
            case ASSET -> ASSET;
            case LIABILITY -> LIABILITY;
        };
    }

    public NetWorthEntryType toDomain() {
        return switch (this) {
            case ASSET -> NetWorthEntryType.ASSET;
            case LIABILITY -> NetWorthEntryType.LIABILITY;
        };
    }
}
