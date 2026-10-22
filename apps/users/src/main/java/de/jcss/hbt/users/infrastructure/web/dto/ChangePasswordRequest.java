package de.jcss.hbt.users.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** The rules for the new password are checked by the domain; the repetition is checked by the frontend (PA-5). */
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
