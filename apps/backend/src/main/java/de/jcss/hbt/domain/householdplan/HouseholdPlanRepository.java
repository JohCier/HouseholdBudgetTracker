package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;

import java.time.Year;
import java.util.Optional;

/**
 * Collection of all {@link HouseholdPlan} aggregates for the write side. Always loads and
 * stores a plan as a whole, including its items. Not meant for reading data for display;
 * that is what the query handlers are for.
 * <p>
 * Keeps whether a plan is active or deleted, and when it was deleted (GP-1, GP-2). All methods
 * but {@link #findDeletedById} only see active plans, so a deleted plan can be neither changed
 * nor copied (GP-3, GP-4).
 */
public interface HouseholdPlanRepository {

    /**
     * Takes a plan out of the collection to change it. Within a transaction, other
     * transactions cannot take the same plan until this one has finished, so concurrent
     * changes to a plan are applied one after the other instead of overwriting each other.
     */
    Optional<HouseholdPlan> findById(HouseholdPlanId id);

    /** Takes the active plan of the owner for the year out of the collection to change it, locked like {@link #findById}. */
    Optional<HouseholdPlan> findFor(OwnerId owner, Year year);

    /** Takes a deleted plan out of the collection to restore or purge it, locked like {@link #findById}. */
    Optional<HouseholdPlan> findDeletedById(HouseholdPlanId id);

    /** Whether the owner already has an active plan for the year. */
    boolean existsFor(OwnerId owner, Year year);

    /**
     * Adds a newly created plan to the collection.
     *
     * @throws HouseholdPlanAlreadyExistsException if the owner already has an active plan for the same year
     */
    void add(HouseholdPlan plan);

    /** Brings a plan taken from this collection via {@link #findById} up to date with its changes. */
    void save(HouseholdPlan plan);

    /** Keeps an active plan as deleted from now on, with all its items (GP-2, GP-7). */
    void markDeleted(HouseholdPlan plan);

    /**
     * Makes a deleted plan active again (GP-11).
     *
     * @throws HouseholdPlanAlreadyExistsException if the owner has an active plan for the same year
     */
    void restore(HouseholdPlan plan);

    /** Removes a deleted plan for good, with all its items (GP-17). */
    void remove(HouseholdPlan plan);
}
