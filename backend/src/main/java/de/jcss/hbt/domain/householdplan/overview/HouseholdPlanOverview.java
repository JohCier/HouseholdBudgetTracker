package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.shared.ReadModel;

import java.time.Year;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Read model of the household plan of one year: its items, total income, total expenses,
 * the resulting balance, and a breakdown per {@link ExpenseCategory} comparing actual
 * spend against the 50/30/20 target share of that year's income.
 * <p>
 * Only sums up the items; percentages and whether a target is met are worked out by the
 * value objects {@link Percentage} and {@link ExpenseCategory}.
 */
@ReadModel
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
     * Derives totals and category breakdowns from the items.
     *
     * @param items in the order they should be shown
     */
    public static HouseholdPlanOverview of(HouseholdPlanId planId, Year year, List<OverviewItem> items) {
        Money totalIncome = Money.ZERO;
        Map<ExpenseCategory, Money> expensesByCategory = new EnumMap<>(ExpenseCategory.class);
        for (OverviewItem item : items) {
            if (item.type() == ItemType.INCOME) {
                totalIncome = totalIncome.add(item.amount());
            } else {
                expensesByCategory.merge(item.category(), item.amount(), Money::add);
            }
        }
        Money totalExpense = expensesByCategory.values().stream().reduce(Money.ZERO, Money::add);
        Money balance = totalIncome.subtract(totalExpense);

        Money income = totalIncome;
        List<CategoryBreakdown> breakdowns = Arrays.stream(ExpenseCategory.values())
                .map(category -> breakdownFor(category, expensesByCategory.getOrDefault(category, Money.ZERO), income))
                .toList();

        return new HouseholdPlanOverview(planId, year, totalIncome, totalExpense, balance,
                totalExpense.compareTo(totalIncome) > 0, breakdowns, items);
    }

    private static CategoryBreakdown breakdownFor(ExpenseCategory category, Money categoryTotal, Money totalIncome) {
        return new CategoryBreakdown(category, categoryTotal, Percentage.shareOf(categoryTotal, totalIncome),
                category.targetPercentageOfIncome(), category.targetDirection(),
                category.isTargetMet(categoryTotal, totalIncome));
    }
}
