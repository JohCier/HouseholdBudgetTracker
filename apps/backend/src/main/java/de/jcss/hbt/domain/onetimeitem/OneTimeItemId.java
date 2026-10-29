package de.jcss.hbt.domain.onetimeitem;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link OneTimeItem} aggregate.
 */
@ValueObject
public record OneTimeItemId(UUID value) {

    public OneTimeItemId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OneTimeItemId generate() {
        return new OneTimeItemId(UUID.randomUUID());
    }

    public static OneTimeItemId of(UUID value) {
        return new OneTimeItemId(value);
    }
}
