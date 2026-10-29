package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;

import java.util.Objects;

/** The id of a newly created plan together with its (still empty) overview. */
public record CreatedHouseholdPlan(HouseholdPlanId id, HouseholdPlanOverview overview) {

    public CreatedHouseholdPlan {
        Objects.requireNonNull(id);
        Objects.requireNonNull(overview);
    }
}
