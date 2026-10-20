package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.shared.QueryHandler;

import java.time.Year;
import java.util.Optional;

/**
 * Loads the {@link HouseholdPlanOverview} and {@link YearlyOverview} read models. Implemented in the persistence
 * infrastructure.
 */
@QueryHandler
public interface HouseholdPlanOverviewQueryHandler {

    /** The overview of the owner's plan of the given year, or empty if they have no plan for it. */
    Optional<HouseholdPlanOverview> findByOwnerAndYear(OwnerId owner, Year year);

    /** All plans of the owner from {@code firstYear} to {@code lastYear} inclusive; empty if they have none. */
    YearlyOverview findYearlyOverviewByOwner(OwnerId owner, Year firstYear, Year lastYear);
}
