package com.example.householdbudgettracker.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Value object summarizing the current state of a {@link Budget}: total income, total
 * expenses, the resulting balance, and a breakdown per {@link ExpenseCategory} comparing
 * actual spend against the 50/30/20 target share of income.
 */
public record BudgetOverview(
        Money totalIncome,
        Money totalExpense,
        Money balance,
        boolean expensesExceedIncome,
        List<CategoryBreakdown> categoryBreakdowns
) {

    public static BudgetOverview of(List<BudgetItem> items) {
        Money totalIncome = sum(items, ItemType.INCOME);
        Money totalExpense = sum(items, ItemType.EXPENSE);
        Money balance = totalIncome.subtract(totalExpense);

        List<CategoryBreakdown> breakdowns = List.of(ExpenseCategory.values()).stream()
                .map(category -> breakdownFor(category, items, totalIncome))
                .toList();

        return new BudgetOverview(totalIncome, totalExpense, balance, totalExpense.compareTo(totalIncome) > 0, breakdowns);
    }

    private static CategoryBreakdown breakdownFor(ExpenseCategory category, List<BudgetItem> items, Money totalIncome) {
        Money categoryTotal = sumByCategory(items, category);
        BigDecimal actualPercentage = percentageOf(categoryTotal, totalIncome);
        boolean withinTarget = actualPercentage.compareTo(BigDecimal.valueOf(category.targetPercentageOfIncome())) <= 0;
        return new CategoryBreakdown(category, categoryTotal, actualPercentage, category.targetPercentageOfIncome(), withinTarget);
    }

    private static BigDecimal percentageOf(Money part, Money whole) {
        if (whole.isZero()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
                .filter(item -> item.type() == ItemType.EXPENSE)
                .filter(item -> item.category().map(c -> c == category).orElse(false))
                .map(BudgetItem::amount)
                .reduce(Money.ZERO, Money::add);
    }
}
