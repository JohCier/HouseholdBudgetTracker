package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;

/**
 * Part of the {@link DeletedPlans} read model: a deleted plan with when it was deleted, how many
 * items it has and its totals (GP-22). There is no change from the previous year.
 */
@QueryModel
public record DeletedPlanEntry(
        HouseholdPlanId planId,
        Year year,
        Instant deletedAt,
        int itemCount,
        Money totalIncome,
        Money totalExpense,
        Money balance,
        boolean expensesExceedIncome
) {

    public DeletedPlanEntry {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }

    /** Takes the totals from the summary of the plan's items. */
    public static DeletedPlanEntry of(HouseholdPlanId planId, Year year, Instant deletedAt, int itemCount,
                                      BudgetSummary summary) {
        return new DeletedPlanEntry(planId, year, deletedAt, itemCount, summary.totalIncome(),
                summary.totalExpense(), summary.balance(), summary.expensesExceedIncome());
    }

    /** Total income per month: the yearly total divided by 12 (JM-3, GP-23). */
    public Money monthlyTotalIncome() {
        return totalIncome.perMonth();
    }

    /** Total expenses per month: the yearly total divided by 12 (JM-3, GP-23). */
    public Money monthlyTotalExpense() {
        return totalExpense.perMonth();
    }

    /** The balance per month: the yearly balance divided by 12 (JM-3, GP-23). */
    public Money monthlyBalance() {
        return balance.perMonth();
    }
}
