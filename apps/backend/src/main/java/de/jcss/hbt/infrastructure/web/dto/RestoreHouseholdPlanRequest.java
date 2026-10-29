package de.jcss.hbt.infrastructure.web.dto;

/**
 * @param overwrite whether an active plan the user has for the year of the restored one may be replaced (GP-14);
 *                  {@code false} if missing
 */
public record RestoreHouseholdPlanRequest(Boolean overwrite) {
}
