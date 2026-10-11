package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.TargetDirection;
import de.jcss.hbt.domain.shared.ReadModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Read model of the household plan of one year: its items, total income, total expenses,
 * the resulting balance, and a breakdown per {@link ExpenseCategory} comparing actual
 * spend against the 50/30/20 target share of that year's income.
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
        BigDecimal actualPercentage = percentageOf(categoryTotal, totalIncome);
        return new CategoryBreakdown(category, categoryTotal, actualPercentage, category.targetPercentageOfIncome(),
                category.targetDirection(), isTargetMet(category, categoryTotal, totalIncome, actualPercentage));
    }

    /**
     * Without income no share can be computed: spending anything then breaks a ceiling,
     * while a savings floor cannot be missed.
     */
    private static boolean isTargetMet(ExpenseCategory category, Money categoryTotal, Money totalIncome,
                                       BigDecimal actualPercentage) {
        if (totalIncome.isZero()) {
            return category.targetDirection() == TargetDirection.AT_LEAST || categoryTotal.isZero();
        }
        return category.targetDirection().isMet(actualPercentage, category.targetPercentageOfIncome());
    }

    private static BigDecimal percentageOf(Money part, Money whole) {
        if (whole.isZero()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.value()
                .multiply(BigDecimal.valueOf(100))
                .divide(whole.value(), 2, RoundingMode.HALF_UP);
    }
}
