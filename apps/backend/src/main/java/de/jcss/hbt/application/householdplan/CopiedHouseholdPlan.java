package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.util.Objects;

/**
 * The id of the plan a source was copied to together with its overview.
 *
 * @param overwritten whether the plan existed before; otherwise it was created by the copy
 */
public record CopiedHouseholdPlan(HouseholdPlanId id, boolean overwritten, HouseholdPlanOverview overview) {

    public CopiedHouseholdPlan {
        Objects.requireNonNull(id);
        Objects.requireNonNull(overview);
    }
}
