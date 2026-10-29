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
 */
public interface HouseholdPlanRepository {

    /**
     * Takes a plan out of the collection to change it. Within a transaction, other
     * transactions cannot take the same plan until this one has finished, so concurrent
     * changes to a plan are applied one after the other instead of overwriting each other.
     */
    Optional<HouseholdPlan> findById(HouseholdPlanId id);

    /** Takes the plan of the owner for the year out of the collection to change it, locked like {@link #findById}. */
    Optional<HouseholdPlan> findFor(OwnerId owner, Year year);

    /** Whether the owner already has a plan for the year. */
    boolean existsFor(OwnerId owner, Year year);

    /**
     * Adds a newly created plan to the collection.
     *
     * @throws HouseholdPlanAlreadyExistsException if the owner already has a plan for the same year
     */
    void add(HouseholdPlan plan);

    /** Brings a plan taken from this collection via {@link #findById} up to date with its changes. */
    void save(HouseholdPlan plan);

    /** Removes a plan taken from this collection via {@link #findById}, with all its items (PL-2). */
    void remove(HouseholdPlan plan);
}
