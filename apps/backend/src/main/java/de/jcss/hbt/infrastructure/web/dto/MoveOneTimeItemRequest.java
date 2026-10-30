package de.jcss.hbt.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** The new neighbors of a one-time item within its area; at least one of them is given. */
public record MoveOneTimeItemRequest(
        @Schema(description = "The item that is to come right before; missing to move the item to the start")
        UUID previousItemId,
        @Schema(description = "The item that is to come right after; missing to move the item to the end")
        UUID nextItemId
) {
}
