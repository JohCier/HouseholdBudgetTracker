package de.jcss.hbt.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

/** Whether the item is to be marked as important ({@code true}) or no longer ({@code false}). */
public record MarkImportantRequest(@NotNull Boolean important) {
}
