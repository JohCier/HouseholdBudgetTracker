package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

/**
 * Part of the {@link HouseholdPlanOverview} read model: one row of a top 5 list, an expense with the
 * amount the list is ordered by (TP-1, TP-2).
 *
 * @param item    the expense
 * @param monthly what the list is ordered by: its saving potential (TP-1) or its monthly amount (TP-2)
 * @param annual  the yearly value next to it (TP-3)
 */
@QueryModel
public record TopItem(OverviewItem item, Money monthly, Money annual) {

    /** The number of rows of a top list (TP-1, TP-2). */
    public static final int LIMIT = 5;
}
