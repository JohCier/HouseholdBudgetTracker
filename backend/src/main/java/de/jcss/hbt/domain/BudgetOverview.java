package de.jcss.hbt.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

/**
 * Value object summarizing the bookings of one month: total income, total expenses, the
 * resulting balance, and a breakdown per {@link ExpenseCategory} comparing actual spend
 * against the 50/30/20 target share of that month's income.
 */
public record BudgetOverview(
        YearMonth month,
        Money totalIncome,
        Money totalExpense,
        Money balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdown> categoryBreakdowns
) {

    /**
     * @throws IllegalArgumentException if an item is not booked in the given month
     */
    public static BudgetOverview of(YearMonth month, List<BudgetItem> items) {
        items.stream()
                .filter(item -> !item.isBookedIn(month))
                .findAny()
                .ifPresent(item -> {
                    throw new IllegalArgumentException("item %s is not booked in %s".formatted(item.id(), month));
                });

        Money totalIncome = sum(items, ItemType.INCOME);
        Money totalExpense = sum(items, ItemType.EXPENSE);
        Money balance = totalIncome.subtract(totalExpense);

        List<CategoryBreakdown> breakdowns = Arrays.stream(ExpenseCategory.values())
                .map(category -> breakdownFor(category, items, totalIncome))
                .toList();

        return new BudgetOverview(month, totalIncome, totalExpense, balance,
                totalExpense.compareTo(totalIncome) > 0, breakdowns);
    }

    private static CategoryBreakdown breakdownFor(ExpenseCategory category, List<BudgetItem> items, Money totalIncome) {
        Money categoryTotal = sumByCategory(items, category);
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

    private static Money sum(List<BudgetItem> items, ItemType type) {
        return items.stream()
                .filter(item -> item.type() == type)
                .map(BudgetItem::amount)
                .reduce(Money.ZERO, Money::add);
    }

    private static Money sumByCategory(List<BudgetItem> items, ExpenseCategory category) {
        return items.stream()
                .filter(item -> item.category().filter(c -> c == category).isPresent())
                .map(BudgetItem::amount)
                .reduce(Money.ZERO, Money::add);
    }
}
