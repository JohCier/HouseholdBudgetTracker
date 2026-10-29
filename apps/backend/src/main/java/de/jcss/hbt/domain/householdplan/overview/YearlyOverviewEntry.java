package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.Percentage;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Objects;

/**
 * Part of the {@link YearlyOverview} read model: the totals of one plan (JU-2) and how they
 * changed from the plan of the previous year (JU-5). A change is {@code null} if there is no
 * plan of the previous year (JU-4) or its figure was zero (JU-6).
 */
@QueryModel
public record YearlyOverviewEntry(
        HouseholdPlanId planId,
        Year year,
        Money totalIncome,
        Money totalExpense,
        Money balance,
        boolean expensesExceedIncome,
        Percentage incomeChange,
        Percentage expenseChange,
        Percentage balanceChange
) {

    public YearlyOverviewEntry {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
    }

    /**
     * Takes the totals from the summary of the plan and the changes from {@link Percentage#changeFrom}.
     *
     * @param previousYear the summary of the plan of the previous year, {@code null} if there is none
     */
    public static YearlyOverviewEntry of(HouseholdPlanId planId, Year year, BudgetSummary summary,
                                         BudgetSummary previousYear) {
        return new YearlyOverviewEntry(planId, year, summary.totalIncome(), summary.totalExpense(),
                summary.balance(), summary.expensesExceedIncome(),
                changeOf(previousYear == null ? null : previousYear.totalIncome(), summary.totalIncome()),
                changeOf(previousYear == null ? null : previousYear.totalExpense(), summary.totalExpense()),
                changeOf(previousYear == null ? null : previousYear.balance(), summary.balance()));
    }

    /** Total income per month: the yearly total divided by 12 (JM-3). */
    public Money monthlyTotalIncome() {
        return totalIncome.perMonth();
    }

    /** Total expenses per month: the yearly total divided by 12 (JM-3). */
    public Money monthlyTotalExpense() {
        return totalExpense.perMonth();
    }

    /** The balance per month: the yearly balance divided by 12, not the difference of the monthly totals (JM-3). */
    public Money monthlyBalance() {
        return balance.perMonth();
    }

    private static Percentage changeOf(Money previous, Money current) {
        return previous == null ? null : Percentage.changeFrom(previous, current).orElse(null);
    }
}
