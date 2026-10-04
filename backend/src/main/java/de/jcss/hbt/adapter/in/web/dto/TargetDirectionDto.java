package de.jcss.hbt.adapter.in.web.dto;

import de.jcss.hbt.domain.TargetDirection;
import io.swagger.v3.oas.annotations.media.Schema;

/** API representation of {@link TargetDirection}. */
@Schema(name = "TargetDirection", enumAsRef = true)
public enum TargetDirectionDto {
    AT_MOST,
    AT_LEAST;

    public static TargetDirectionDto from(TargetDirection direction) {
        return switch (direction) {
            case AT_MOST -> AT_MOST;
            case AT_LEAST -> AT_LEAST;
        };
    }
}
