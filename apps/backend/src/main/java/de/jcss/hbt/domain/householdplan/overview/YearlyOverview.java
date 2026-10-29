package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read model of all plans of a user, the newest year first (JU-1), each with its totals and their
 * change from the plan of the immediately preceding year (JU-4, JU-5). The figures come from
 * {@link BudgetSummary} and {@link de.jcss.hbt.domain.householdplan.valueobject.Percentage#changeFrom}.
 */
@QueryModel
public record YearlyOverview(List<YearlyOverviewEntry> years) {

    public YearlyOverview {
        years = List.copyOf(years);
    }

    /** A plan with the summary of its items, as the query handler loads it. */
    public record PlanSummary(HouseholdPlanId planId, Year year, BudgetSummary summary) {

        public PlanSummary {
            Objects.requireNonNull(planId, "planId must not be null");
            Objects.requireNonNull(year, "year must not be null");
            Objects.requireNonNull(summary, "summary must not be null");
        }
    }

    /** Compares every plan with the plan of the year before, if the user has one; plans of other years never. */
    public static YearlyOverview of(List<PlanSummary> plans) {
        Map<Year, BudgetSummary> summaryByYear = plans.stream()
                .collect(Collectors.toMap(PlanSummary::year, PlanSummary::summary));
        return new YearlyOverview(plans.stream()
                .sorted(Comparator.comparing(PlanSummary::year).reversed())
                .map(plan -> YearlyOverviewEntry.of(plan.planId(), plan.year(), plan.summary(),
                        summaryByYear.get(plan.year().minusYears(1))))
                .toList());
    }
}
