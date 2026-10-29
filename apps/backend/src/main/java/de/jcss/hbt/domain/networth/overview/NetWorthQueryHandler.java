package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.shared.QueryHandler;

import java.time.Year;

/**
 * Loads the {@link NetWorthOverview} and {@link NetWorthHistory} read models. Implemented in the persistence
 * infrastructure.
 */
@QueryHandler
public interface NetWorthQueryHandler {

    /** The assets and liabilities of the owner for the year; without entries if they recorded none. */
    NetWorthOverview findByOwnerAndYear(OwnerId owner, Year year);

    /** All years from {@code firstYear} to {@code lastYear} inclusive the owner has a statement for. */
    NetWorthHistory findHistoryByOwner(OwnerId owner, Year firstYear, Year lastYear);
}
