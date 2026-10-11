package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;

import java.time.Year;
import java.util.Optional;

/**
 * Collection-like access to {@link HouseholdPlan} aggregates for the write side. Always
 * loads and saves a plan as a whole, including its items. Not meant for reading data
 * for display; that is what the query handlers are for.
 */
public interface HouseholdPlanRepository {

    Optional<HouseholdPlan> findById(HouseholdPlanId id);

    boolean existsForYear(Year year);

    /**
     * Inserts a new plan or brings a stored one up to date with the given state.
     *
     * @throws HouseholdPlanAlreadyExistsException if another plan for the same year exists
     */
    void save(HouseholdPlan plan);
}
