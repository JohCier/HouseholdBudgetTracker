package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The shares per month the pie of a category or subcategory shows (TK-6, TK-8, TK-9). */
class HouseholdPlanOverviewSharesTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final Year YEAR = Year.of(2026);

    /** TK-6, TK-8 */
    @Test
    void groupsShowTheirShareOfTheCategoryAndItemsTheirShareOfTheGroup() {
        OverviewItem rent = monthly("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", "1000.00");
        OverviewItem fuel = monthly("Sprit", ExpenseCategory.LEBENSUNTERHALT, "Auto", "200.00");
        OverviewItem insurance = monthly("Versicherung", ExpenseCategory.LEBENSUNTERHALT, "Auto", "50.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(rent, fuel, insurance),
                Map.of());

        OverviewCategoryItems living = overview.expenseCategories().getFirst();
        OverviewItemGroup car = living.groups().getFirst();
        assertThat(car.subcategory()).isEqualTo(BudgetItemSubcategory.of("Auto"));
        assertThat(car.percentageOfSection()).isEqualTo(Percentage.of(20));
        assertThat(living.groups().getLast().percentageOfSection()).isEqualTo(Percentage.of(80));
        assertThat(car.percentageOf(fuel)).isEqualTo(Percentage.of(80));
        assertThat(car.percentageOf(insurance)).isEqualTo(Percentage.of(20));
    }

    /** TK-6: a yearly item counts with its amount per month */
    @Test
    void yearlyItemsCountWithTheirMonthlyAmount() {
        OverviewItem tax = new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Kfz-Steuer"),
                ItemAmount.yearly(money("120.00")), ItemType.EXPENSE, ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.of("Auto"), null);
        OverviewItem fuel = monthly("Sprit", ExpenseCategory.LEBENSUNTERHALT, "Auto", "30.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(tax, fuel), Map.of());

        OverviewItemGroup car = overview.expenseCategories().getFirst().groups().getFirst();
        assertThat(car.percentageOf(tax)).isEqualTo(Percentage.of(25));
        assertThat(car.percentageOf(fuel)).isEqualTo(Percentage.of(75));
    }

    /** TK-9: the share of a savings group refers to the total including the unplanned income */
    @Test
    void savingsSharesIncludeTheUnplannedIncome() {
        OverviewItem salary = new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Gehalt"),
                ItemAmount.monthly(money("3000.00")), ItemType.INCOME, null, null, null);
        OverviewItem etf = monthly("ETF", ExpenseCategory.SPAREN_INVESTIEREN, "Investieren", "500.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(salary, etf), Map.of());

        assertThat(overview.expenseCategories().getFirst().groups().getFirst().percentageOfSection())
                .isEqualTo(new Percentage(new BigDecimal("16.67")));
        CategoryBreakdown savings = overview.categoryBreakdowns().getLast();
        assertThat(savings.monthlyUnplanned()).isEqualTo(money("2500.00"));
        assertThat(savings.monthlyUnplannedShare()).isEqualTo(new Percentage(new BigDecimal("83.33")));
        assertThat(overview.incomeGroups().getFirst().percentageOfSection()).isEqualTo(Percentage.of(100));
    }

    private static OverviewItem monthly(String description, ExpenseCategory category, String subcategory,
                                        String amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description),
                ItemAmount.monthly(money(amount)), ItemType.EXPENSE, category,
                BudgetItemSubcategory.ofNullable(subcategory), null);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
