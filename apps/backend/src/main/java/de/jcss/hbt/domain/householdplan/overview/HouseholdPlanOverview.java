package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.BudgetSummary;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import de.jcss.hbt.domain.householdplan.SavingPotential;
import de.jcss.hbt.domain.householdplan.SubcategoryOrder;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read model of the household plan of one year: its items, total income, total expenses,
 * the resulting balance, and a breakdown per {@link ExpenseCategory} comparing actual
 * spend against the 50/30/20 target share of that year's income. All figures are yearly
 * figures, worked out from the yearly amounts of the items (MJ-8); the sums the entries and
 * tiles show come per month, too (MJ-9). The items come once as a
 * plain list and once grouped as the entries show them: the incomes by subcategory, the
 * expenses by category and subcategory, each with its sum (UK-9 to UK-13) and its share per month
 * of the section, as the pie of a category or subcategory shows it (TK-6, TK-8). The saving potential of
 * the plan is the one exception to yearly figures: it is the sum of the monthly saving potentials of
 * its expenses, and twelve times that for the year (PM-13), also per subcategory (SU-1).
 * <p>
 * Contains no business logic of its own: all figures are worked out by the value object
 * {@link BudgetSummary}, which the read model only fills with the items; the order of the groups
 * comes from {@link SubcategoryOrder#arrange}, the sums of the groups from {@link Money#add}.
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
        List<OverviewItem> items,
        List<OverviewItemGroup> incomeGroups,
        List<OverviewCategoryItems> expenseCategories
) {

    public HouseholdPlanOverview {
        categoryBreakdowns = List.copyOf(categoryBreakdowns);
        items = List.copyOf(items);
        incomeGroups = List.copyOf(incomeGroups);
        expenseCategories = List.copyOf(expenseCategories);
    }

    /**
     * Takes totals and category breakdowns from the {@link BudgetSummary} of the items.
     *
     * @param items             in display order, the topmost first
     * @param subcategoryOrders the arranged order of the groups per section; sections never arranged are missing
     */
    public static HouseholdPlanOverview of(HouseholdPlanId planId, Year year, List<OverviewItem> items,
                                           Map<ItemSection, SubcategoryOrder> subcategoryOrders) {
        BudgetSummary summary = BudgetSummary.EMPTY;
        for (OverviewItem item : items) {
            summary = item.type() == ItemType.INCOME
                    ? summary.withIncome(item.amount().annual())
                    : summary.withExpense(item.category(), item.amount().annual());
        }
        BudgetSummary result = summary;
        List<CategoryBreakdown> breakdowns = Arrays.stream(ExpenseCategory.values())
                .map(category -> CategoryBreakdown.of(category, result))
                .toList();
        List<OverviewCategoryItems> expenseCategories = breakdowns.stream()
                .map(breakdown -> {
                    ExpenseCategory category = breakdown.category();
                    List<OverviewItemGroup> groups = groupsOf(ItemSection.expenses(category), items, subcategoryOrders,
                            breakdown.monthlyTotal());
                    return new OverviewCategoryItems(category, totalOf(groups), groups);
                })
                .filter(category -> !category.groups().isEmpty())
                .toList();
        return new HouseholdPlanOverview(planId, year, summary.totalIncome(), summary.totalExpense(),
                summary.balance(), summary.expensesExceedIncome(), breakdowns, items,
                groupsOf(ItemSection.INCOME, items, subcategoryOrders, summary.totalIncome().perMonth()),
                expenseCategories);
    }

    /** Total income per month: the yearly total divided by 12 (MJ-9). */
    public Money monthlyTotalIncome() {
        return totalIncome.perMonth();
    }

    /** Total expenses per month: the yearly total divided by 12 (MJ-9). */
    public Money monthlyTotalExpense() {
        return totalExpense.perMonth();
    }

    /** The balance per month: the yearly balance divided by 12 (MJ-9). */
    public Money monthlyBalance() {
        return balance.perMonth();
    }

    /** The saving potential per month: the sum of the saving potentials of all expenses that have one (PM-13). */
    public Money monthlySavingPotential() {
        return items.stream()
                .map(item -> item.marks().savingPotential())
                .filter(Objects::nonNull)
                .map(SavingPotential::monthly)
                .reduce(Money.ZERO, Money::add);
    }

    /** The saving potential for the whole year: twelve times the monthly one (PM-13). */
    public Money savingPotential() {
        return monthlySavingPotential().timesMonthsPerYear();
    }

    /**
     * The saving potential per subcategory, across all categories (SU-2): only subcategories with a saving
     * potential (SU-3), the largest first, equal ones alphabetically (SU-4). A subcategory takes the spelling
     * of its first group in the entries.
     */
    public List<SubcategorySavingPotential> savingPotentialsBySubcategory() {
        List<SubcategorySavingPotential> sums = new ArrayList<>();
        for (OverviewCategoryItems category : expenseCategories) {
            for (OverviewItemGroup group : category.groups()) {
                Money monthly = group.items().stream()
                        .map(item -> item.marks().savingPotential())
                        .filter(Objects::nonNull)
                        .map(SavingPotential::monthly)
                        .reduce(Money.ZERO, Money::add);
                if (monthly.isZero()) {
                    continue;
                }
                int known = indexOf(sums, group.subcategory());
                if (known < 0) {
                    sums.add(new SubcategorySavingPotential(group.subcategory(), monthly));
                } else {
                    SubcategorySavingPotential sum = sums.get(known);
                    sums.set(known, new SubcategorySavingPotential(sum.subcategory(), sum.monthly().add(monthly)));
                }
            }
        }
        return sums.stream()
                .sorted(Comparator.comparing(SubcategorySavingPotential::monthly).reversed()
                        .thenComparing(sum -> sum.subcategory().value(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static int indexOf(List<SubcategorySavingPotential> sums, BudgetItemSubcategory subcategory) {
        for (int i = 0; i < sums.size(); i++) {
            if (sums.get(i).subcategory().isSameAs(subcategory)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The groups of a section in display order, each with its items in display order (UK-11, UK-16).
     *
     * @param sectionMonthlyTotal what the share of a group refers to: the income, or the total of the
     *                            category including its unplanned part (TK-9)
     */
    private static List<OverviewItemGroup> groupsOf(ItemSection section, List<OverviewItem> items,
                                                    Map<ItemSection, SubcategoryOrder> subcategoryOrders,
                                                    Money sectionMonthlyTotal) {
        List<OverviewItem> inSection = items.stream().filter(item -> item.section().equals(section)).toList();
        List<BudgetItemSubcategory> groups = new ArrayList<>();
        for (OverviewItem item : inSection) {
            if (groups.stream().noneMatch(item.group()::isSameAs)) {
                groups.add(item.group());
            }
        }
        return subcategoryOrders.getOrDefault(section, SubcategoryOrder.NONE).arrange(groups).stream()
                .map(group -> {
                    List<OverviewItem> inGroup = inSection.stream().filter(item -> item.group().isSameAs(group)).toList();
                    Money total = inGroup.stream().map(item -> item.amount().annual()).reduce(Money.ZERO, Money::add);
                    Percentage share = Percentage.shareOf(total.perMonth(), sectionMonthlyTotal);
                    return new OverviewItemGroup(group, total, share, inGroup);
                })
                .toList();
    }

    private static Money totalOf(List<OverviewItemGroup> groups) {
        return groups.stream().map(OverviewItemGroup::total).reduce(Money.ZERO, Money::add);
    }
}
