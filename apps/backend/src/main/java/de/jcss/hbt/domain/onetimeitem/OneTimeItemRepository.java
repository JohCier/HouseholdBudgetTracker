package de.jcss.hbt.domain.onetimeitem;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import java.util.Optional;

/**
 * Collection of all {@link OneTimeItem} aggregates for the write side. Not meant for reading
 * data for display; that is what the query handler of the overview is for.
 */
public interface OneTimeItemRepository {

    /**
     * Takes an item out of the collection to change it. Within a transaction, other
     * transactions cannot take the same item until this one has finished.
     */
    Optional<OneTimeItem> findById(OneTimeItemId id);

    /** Adds a newly created item to the collection. */
    void add(OneTimeItem item);

    /** Brings an item taken from this collection via {@link #findById} up to date with its changes. */
    void save(OneTimeItem item);

    /** Removes an item taken from this collection via {@link #findById}. */
    void remove(OneTimeItem item);
}
