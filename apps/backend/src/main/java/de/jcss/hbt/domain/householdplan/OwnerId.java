package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.shared.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of the user a {@link HouseholdPlan} belongs to. Users are managed by the users
 * app; this context only knows them by their id.
 */
@ValueObject
public record OwnerId(UUID value) {

    public OwnerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OwnerId of(UUID value) {
        return new OwnerId(value);
    }
}
