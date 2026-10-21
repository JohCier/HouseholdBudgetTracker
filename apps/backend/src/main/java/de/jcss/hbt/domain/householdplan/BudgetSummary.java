package de.jcss.hbt.domain.householdplan;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The financial position of a household plan: its total income and its expenses per
 * {@link ExpenseCategory}, together with what follows from them, i.e. the balance and
 * whether each category keeps to its 50/30/20 target share of income.
 * <p>
 * Income that no expense uses up, the {@linkplain #surplus() surplus}, counts towards
 * {@link #SURPLUS_CATEGORY} when its share and target are assessed; totals and balance only
 * contain what was actually entered.
 * <p>
 * Built up item by item starting from {@link #EMPTY}; every category is always present,
 * those without expenses with {@link Money#ZERO}. Income and expenses are never negative.
 */
@ValueObject
public record BudgetSummary(Money totalIncome, Map<ExpenseCategory, Money> expensesByCategory) {

    public static final BudgetSummary EMPTY = new BudgetSummary(Money.ZERO, Map.of());

    /** The category the surplus counts towards. */
    public static final ExpenseCategory SURPLUS_CATEGORY = ExpenseCategory.FREIZEIT_SPASS;

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

    /** Income that no expense uses up; zero if expenses equal or exceed the income. */
    public Money surplus() {
        Money balance = balance();
        return balance.compareTo(Money.ZERO) > 0 ? balance : Money.ZERO;
    }

    public Money expensesIn(ExpenseCategory category) {
        return expensesByCategory.get(category);
    }

    /** The surplus for {@link #SURPLUS_CATEGORY}, zero for every other category. */
    public Money unplannedIn(ExpenseCategory category) {
        return category == SURPLUS_CATEGORY ? surplus() : Money.ZERO;
    }

    /** What the category is assessed with: its expenses plus the surplus it takes up. */
    public Money assessedIn(ExpenseCategory category) {
        return expensesIn(category).add(unplannedIn(category));
    }

    /** Share of the income assessed for the category; zero without income. */
    public Percentage shareOfIncome(ExpenseCategory category) {
        return Percentage.shareOf(assessedIn(category), totalIncome);
    }

    /** Whether the amount assessed for the category keeps to its target share of the income. */
    public boolean isTargetMet(ExpenseCategory category) {
        return category.isTargetMet(assessedIn(category), totalIncome);
    }
}
