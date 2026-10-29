package de.jcss.hbt.domain.networth.valueobject;

import de.jcss.hbt.domain.networth.NetWorthStatement;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link NetWorthStatement} aggregate.
 */
@ValueObject
public record NetWorthStatementId(UUID value) {

    public NetWorthStatementId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static NetWorthStatementId generate() {
        return new NetWorthStatementId(UUID.randomUUID());
    }

    public static NetWorthStatementId of(UUID value) {
        return new NetWorthStatementId(value);
    }
}
