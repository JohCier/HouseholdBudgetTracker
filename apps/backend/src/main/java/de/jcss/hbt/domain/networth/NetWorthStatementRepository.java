package de.jcss.hbt.domain.networth;

import de.jcss.hbt.domain.exception.NetWorthStatementAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;

import java.time.Year;
import java.util.Optional;

/**
 * Collection of all {@link NetWorthStatement} aggregates for the write side. Always loads and stores
 * a statement as a whole, including its entries. Not meant for reading data for display; that is what
 * the query handler is for.
 */
public interface NetWorthStatementRepository {

    /**
     * Takes the statement of the owner for the year out of the collection to change it. Within a
     * transaction, other transactions cannot take the same statement until this one has finished.
     */
    Optional<NetWorthStatement> findFor(OwnerId owner, Year year);

    /**
     * Adds a newly created statement to the collection.
     *
     * @throws NetWorthStatementAlreadyExistsException if the owner already has a statement for the same year
     */
    void add(NetWorthStatement statement);

    /** Brings a statement taken from this collection up to date with its changes. */
    void save(NetWorthStatement statement);

    /** Removes a statement taken from this collection, with all its entries (VS-7). */
    void remove(NetWorthStatement statement);
}
