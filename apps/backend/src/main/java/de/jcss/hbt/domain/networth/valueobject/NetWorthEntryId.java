package de.jcss.hbt.domain.networth.valueobject;

import de.jcss.hbt.domain.networth.NetWorthEntry;
import de.jcss.hbt.domain.networth.NetWorthStatement;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link NetWorthEntry} entity within the {@link NetWorthStatement} aggregate.
 */
@ValueObject
public record NetWorthEntryId(UUID value) {

    public NetWorthEntryId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static NetWorthEntryId generate() {
        return new NetWorthEntryId(UUID.randomUUID());
    }

    public static NetWorthEntryId of(UUID value) {
        return new NetWorthEntryId(value);
    }
}
