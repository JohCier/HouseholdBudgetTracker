package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.Percentage;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Objects;

/**
 * Part of the {@link YearlyOverview} read model: the totals of the plan of one year (JU-2), how they
 * changed from the plan of the previous year (JU-5), and the totals of the one-time items of the
 * year (EP-13). A change is {@code null} if there is no plan of the previous year (JU-4) or its
 * figure was zero (JU-6).
 * <p>
 * A year with one-time items but no plan has no plan id and {@code null} plan totals and changes
 * (EP-12). The one-time totals are always yearly amounts and never compared with the previous
 * year (EP-14, EP-15); without one-time items they are zero.
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
        Percentage balanceChange,
        Money oneTimeIncome,
        Money oneTimeExpense
) {

    public YearlyOverviewEntry {
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(oneTimeIncome, "oneTimeIncome must not be null");
        Objects.requireNonNull(oneTimeExpense, "oneTimeExpense must not be null");
    }

    /**
     * Takes the totals from the summary of the plan and the changes from {@link Percentage#changeFrom}.
     *
     * @param previousYear the summary of the plan of the previous year, {@code null} if there is none
     */
    public static YearlyOverviewEntry of(HouseholdPlanId planId, Year year, BudgetSummary summary,
                                         BudgetSummary previousYear, Money oneTimeIncome, Money oneTimeExpense) {
        Objects.requireNonNull(planId, "planId must not be null");
        return new YearlyOverviewEntry(planId, year, summary.totalIncome(), summary.totalExpense(),
                summary.balance(), summary.expensesExceedIncome(),
                changeOf(previousYear == null ? null : previousYear.totalIncome(), summary.totalIncome()),
                changeOf(previousYear == null ? null : previousYear.totalExpense(), summary.totalExpense()),
                changeOf(previousYear == null ? null : previousYear.balance(), summary.balance()),
                oneTimeIncome, oneTimeExpense);
    }

    /** A year with one-time items, but without a plan (EP-12). */
    public static YearlyOverviewEntry withoutPlan(Year year, Money oneTimeIncome, Money oneTimeExpense) {
        return new YearlyOverviewEntry(null, year, null, null, null, false, null, null, null,
                oneTimeIncome, oneTimeExpense);
    }

    /** Whether the user has a plan for the year, not only one-time items (EP-12). */
    public boolean hasPlan() {
        return planId != null;
    }

    /** Total income per month: the yearly total divided by 12 (JM-3); {@code null} without a plan. */
    public Money monthlyTotalIncome() {
        return totalIncome == null ? null : totalIncome.perMonth();
    }

    /** Total expenses per month: the yearly total divided by 12 (JM-3); {@code null} without a plan. */
    public Money monthlyTotalExpense() {
        return totalExpense == null ? null : totalExpense.perMonth();
    }

    /**
     * The balance per month: the yearly balance divided by 12, not the difference of the monthly totals (JM-3);
     * {@code null} without a plan.
     */
    public Money monthlyBalance() {
        return balance == null ? null : balance.perMonth();
    }

    /** One-time incomes minus one-time expenses of the year (EP-13). */
    public Money oneTimeBalance() {
        return oneTimeIncome.subtract(oneTimeExpense);
    }

    /** Whether the one-time expenses of the year are higher than its one-time incomes (EP-13). */
    public boolean oneTimeExpensesExceedIncome() {
        return oneTimeExpense.compareTo(oneTimeIncome) > 0;
    }

    private static Percentage changeOf(Money previous, Money current) {
        return previous == null ? null : Percentage.changeFrom(previous, current).orElse(null);
    }
}
