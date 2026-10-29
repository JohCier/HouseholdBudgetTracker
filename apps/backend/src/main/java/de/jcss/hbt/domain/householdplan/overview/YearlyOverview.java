package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read model of all years of a user with a plan or one-time items, the newest year first (JU-1,
 * EP-12). Each year has the totals of its plan and their change from the plan of the immediately
 * preceding year (JU-4, JU-5), and the totals of its one-time items (EP-13). The figures of a plan
 * come from {@link BudgetSummary} and {@link de.jcss.hbt.domain.householdplan.Percentage#changeFrom}.
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

    /** The totals of the one-time items of a year, as the query handler loads them (EP-13). */
    public record OneTimeSummary(Year year, Money income, Money expense) {

        public OneTimeSummary {
            Objects.requireNonNull(year, "year must not be null");
            Objects.requireNonNull(income, "income must not be null");
            Objects.requireNonNull(expense, "expense must not be null");
        }
    }

    /** Plans only, as if the user had no one-time items. */
    public static YearlyOverview of(List<PlanSummary> plans) {
        return of(plans, List.of());
    }

    /**
     * Compares every plan with the plan of the year before, if the user has one; plans of other
     * years never, and a year with only one-time items does not count as a plan (JU-4, EP-15).
     */
    public static YearlyOverview of(List<PlanSummary> plans, List<OneTimeSummary> oneTimeItems) {
        Map<Year, PlanSummary> planByYear = plans.stream()
                .collect(Collectors.toMap(PlanSummary::year, Function.identity()));
        Map<Year, OneTimeSummary> oneTimeByYear = oneTimeItems.stream()
                .collect(Collectors.toMap(OneTimeSummary::year, Function.identity()));
        TreeSet<Year> years = new TreeSet<>(Comparator.reverseOrder());
        years.addAll(planByYear.keySet());
        years.addAll(oneTimeByYear.keySet());
        return new YearlyOverview(years.stream()
                .map(year -> entryOf(year, planByYear.get(year), planByYear.get(year.minusYears(1)),
                        oneTimeByYear.get(year)))
                .toList());
    }

    private static YearlyOverviewEntry entryOf(Year year, PlanSummary plan, PlanSummary previousYear,
                                               OneTimeSummary oneTime) {
        Money oneTimeIncome = oneTime == null ? Money.ZERO : oneTime.income();
        Money oneTimeExpense = oneTime == null ? Money.ZERO : oneTime.expense();
        if (plan == null) {
            return YearlyOverviewEntry.withoutPlan(year, oneTimeIncome, oneTimeExpense);
        }
        return YearlyOverviewEntry.of(plan.planId(), year, plan.summary(),
                previousYear == null ? null : previousYear.summary(), oneTimeIncome, oneTimeExpense);
    }
}
