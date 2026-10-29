package de.jcss.hbt.domain.onetimeitem.event;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** A user entered a one-time income or expense on the given date. */
public record OneTimeItemCreated(OneTimeItemId itemId, OwnerId owner, LocalDate date, Instant timestamp)
        implements OneTimeItemEvent {

    public OneTimeItemCreated {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
