package de.jcss.hbt.users.infrastructure.web.dto;

import java.util.UUID;

/** A signed-in user. */
public record UserResponse(UUID id, String email) {
}
