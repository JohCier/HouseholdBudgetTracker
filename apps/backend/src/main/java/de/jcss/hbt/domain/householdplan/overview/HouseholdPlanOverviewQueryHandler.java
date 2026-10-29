package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.shared.QueryHandler;

import java.time.Year;
import java.util.Optional;

/**
 * Loads the {@link HouseholdPlanOverview}, {@link YearlyOverview} and {@link DeletedPlans} read models. Implemented in the persistence
 * infrastructure.
 */
@QueryHandler
public interface HouseholdPlanOverviewQueryHandler {

    /** The overview of the owner's active plan of the given year, or empty if they have none for it (GP-3). */
    Optional<HouseholdPlanOverview> findByOwnerAndYear(OwnerId owner, Year year);

    /**
     * All years from {@code firstYear} to {@code lastYear} inclusive in which the owner has an active plan or
     * one-time items (EP-12, GP-3), empty if there are none, and the number of their deleted plans in that range
     * (GP-21).
     */
    YearlyOverview findYearlyOverviewByOwner(OwnerId owner, Year firstYear, Year lastYear);

    /** All deleted plans of the owner from {@code firstYear} to {@code lastYear} inclusive (GP-22). */
    DeletedPlans findDeletedPlansByOwner(OwnerId owner, Year firstYear, Year lastYear);
}
