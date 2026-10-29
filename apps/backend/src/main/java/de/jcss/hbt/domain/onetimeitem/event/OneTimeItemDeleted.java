package de.jcss.hbt.domain.onetimeitem.event;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;

import java.time.Instant;
import java.util.Objects;

/** A one-time item was deleted for good (EP-17). */
public record OneTimeItemDeleted(OneTimeItemId itemId, Instant timestamp) implements OneTimeItemEvent {

    public OneTimeItemDeleted {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
