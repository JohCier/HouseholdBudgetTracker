package de.jcss.hbt.users.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** The rules for e-mail address and password are checked by the domain. */
public record RegisterUserRequest(@NotBlank String email, @NotBlank String password) {
}
