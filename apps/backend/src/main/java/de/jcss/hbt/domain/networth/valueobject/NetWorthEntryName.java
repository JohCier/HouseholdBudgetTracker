package de.jcss.hbt.domain.networth.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.networth.NetWorthEntry;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * What a {@link NetWorthEntry} stands for, e.g. "Girokonto" (VS-1). Must not be blank and at most
 * {@value #MAX_LENGTH} characters long, like the description of a budget item.
 */
@ValueObject
public record NetWorthEntryName(String value) {

    public static final int MAX_LENGTH = 255;

    public NetWorthEntryName {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("name must not be longer than %d characters".formatted(MAX_LENGTH));
        }
    }

    public static NetWorthEntryName of(String value) {
        return new NetWorthEntryName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
