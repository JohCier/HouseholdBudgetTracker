package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;

import java.util.Objects;

/** The id of a newly added item together with the overview of its plan after adding it. */
public record AddedBudgetItem(BudgetItemId id, HouseholdPlanOverview overview) {

    public AddedBudgetItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(overview);
    }
}
