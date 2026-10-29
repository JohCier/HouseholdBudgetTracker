package de.jcss.hbt.domain.onetimeitem.event;

import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import de.jcss.hbt.domain.shared.TimestampedDomainEvent;

/** A domain event about a change to one one-time item. */
public interface OneTimeItemEvent extends TimestampedDomainEvent {

    /** The item that was changed. */
    OneTimeItemId itemId();
}
