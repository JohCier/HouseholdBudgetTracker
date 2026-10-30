package de.jcss.hbt.domain.onetimeitem.event;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;

import java.time.Instant;
import java.util.Objects;

/** A one-time item was moved to another place within its area (ES-4). */
public record OneTimeItemMoved(OneTimeItemId itemId, Instant timestamp) implements OneTimeItemEvent {

    public OneTimeItemMoved {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
