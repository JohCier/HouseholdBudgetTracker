package de.jcss.hbt.domain.onetimeitem.event;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** A one-time item was changed; its date afterwards, which may lie in another year (EP-7). */
public record OneTimeItemChanged(OneTimeItemId itemId, LocalDate date, Instant timestamp) implements OneTimeItemEvent {

    public OneTimeItemChanged {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
