package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.shared.ValueObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The financial position of a household plan: its total income and its expenses per
 * {@link ExpenseCategory}, together with what follows from them, i.e. the balance and
 * whether each category keeps to its 50/30/20 target share of income.
 * <p>
 * Built up item by item starting from {@link #EMPTY}; every category is always present,
 * those without expenses with {@link Money#ZERO}. Income and expenses are never negative.
 */
@ValueObject
public record BudgetSummary(Money totalIncome, Map<ExpenseCategory, Money> expensesByCategory) {

    public static final BudgetSummary EMPTY = new BudgetSummary(Money.ZERO, Map.of());

    public BudgetSummary {
        Objects.requireNonNull(totalIncome, "totalIncome must not be null");
        Objects.requireNonNull(expensesByCategory, "expensesByCategory must not be null");
        requireNotNegative(totalIncome, "totalIncome");
        Map<ExpenseCategory, Money> complete = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            Money expenses = expensesByCategory.getOrDefault(category, Money.ZERO);
            requireNotNegative(expenses, "expenses of " + category);
            complete.put(category, expenses);
        }
        expensesByCategory = Map.copyOf(complete);
    }

    private static void requireNotNegative(Money amount, String name) {
        Objects.requireNonNull(amount, name + " must not be null");
        if (amount.compareTo(Money.ZERO) < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    public BudgetSummary withIncome(Money amount) {
        return new BudgetSummary(totalIncome.add(amount), expensesByCategory);
    }

    public BudgetSummary withExpense(ExpenseCategory category, Money amount) {
        Objects.requireNonNull(category, "category must not be null");
        Map<ExpenseCategory, Money> expenses = new EnumMap<>(expensesByCategory);
        expenses.merge(category, amount, Money::add);
        return new BudgetSummary(totalIncome, expenses);
    }

    public Money totalExpense() {
        return expensesByCategory.values().stream().reduce(Money.ZERO, Money::add);
    }

    /** Income minus expenses; negative if more is spent than earned. */
    public Money balance() {
        return totalIncome.subtract(totalExpense());
    }

    public boolean expensesExceedIncome() {
        return totalExpense().compareTo(totalIncome) > 0;
    }

    public Money expensesIn(ExpenseCategory category) {
        return expensesByCategory.get(category);
    }

    /** Share of the income spent in the category; zero without income. */
    public Percentage shareOfIncome(ExpenseCategory category) {
        return Percentage.shareOf(expensesIn(category), totalIncome);
    }

    /** Whether the expenses in the category keep to its target share of the income. */
    public boolean isTargetMet(ExpenseCategory category) {
        return category.isTargetMet(expensesIn(category), totalIncome);
    }
}
