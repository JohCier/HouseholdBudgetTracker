package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Arrays;
import java.util.List;

/**
 * Read model of the household plan of one year: its items, total income, total expenses,
 * the resulting balance, and a breakdown per {@link ExpenseCategory} comparing actual
 * spend against the 50/30/20 target share of that year's income.
 * <p>
 * Contains no business logic of its own: all figures are worked out by the value object
 * {@link BudgetSummary}, which the read model only fills with the items.
 */
@QueryModel
public record HouseholdPlanOverview(
        HouseholdPlanId planId,
        Year year,
        Money totalIncome,
        Money totalExpense,
        Money balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdown> categoryBreakdowns,
        List<OverviewItem> items
) {

    public HouseholdPlanOverview {
        categoryBreakdowns = List.copyOf(categoryBreakdowns);
        items = List.copyOf(items);
    }

    /**
     * Takes totals and category breakdowns from the {@link BudgetSummary} of the items.
     *
     * @param items in the order they should be shown
     */
    public static HouseholdPlanOverview of(HouseholdPlanId planId, Year year, List<OverviewItem> items) {
        BudgetSummary summary = BudgetSummary.EMPTY;
        for (OverviewItem item : items) {
            summary = item.type() == ItemType.INCOME
                    ? summary.withIncome(item.amount())
                    : summary.withExpense(item.category(), item.amount());
        }
        BudgetSummary result = summary;
        List<CategoryBreakdown> breakdowns = Arrays.stream(ExpenseCategory.values())
                .map(category -> CategoryBreakdown.of(category, result))
                .toList();
        return new HouseholdPlanOverview(planId, year, summary.totalIncome(), summary.totalExpense(),
                summary.balance(), summary.expensesExceedIncome(), breakdowns, items);
    }
}
