package de.jcss.hbt.domain.onetimeitem.overview;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.shared.QueryHandler;

import java.time.Year;

/**
 * Loads the {@link OneTimeItemOverview} read model. Implemented in the persistence infrastructure.
 */
@QueryHandler
public interface OneTimeItemOverviewQueryHandler {

    /** The one-time items of the owner whose date lies in the given year; without items if there are none. */
    OneTimeItemOverview findByOwnerAndYear(OwnerId owner, Year year);
}
