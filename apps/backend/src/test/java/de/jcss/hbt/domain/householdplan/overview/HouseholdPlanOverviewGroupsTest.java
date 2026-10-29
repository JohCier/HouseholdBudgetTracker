package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.SubcategoryOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The entries of the overview grouped by section, category and subcategory (UK-9 to UK-13). */
class HouseholdPlanOverviewGroupsTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final Year YEAR = Year.of(2026);

    /** UK-11, UK-12, UK-16 */
    @Test
    void groupsTheExpensesOfACategoryBySubcategoryWithSums() {
        OverviewItem maintenance = expense("Wartung", ExpenseCategory.LEBENSUNTERHALT, "Auto", "400.00");
        OverviewItem tax = expense("Steuer", ExpenseCategory.LEBENSUNTERHALT, "Auto", "150.00");
        OverviewItem fuel = expense("Sprit", ExpenseCategory.LEBENSUNTERHALT, "Auto", "1200.00");
        OverviewItem rent = expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", "9600.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR,
                List.of(rent, fuel, tax, maintenance), Map.of());

        assertThat(overview.expenseCategories()).hasSize(1);
        OverviewCategoryItems living = overview.expenseCategories().getFirst();
        assertThat(living.category()).isEqualTo(ExpenseCategory.LEBENSUNTERHALT);
        assertThat(living.total()).isEqualTo(money("11350.00"));
        assertThat(living.groups()).extracting(OverviewItemGroup::subcategory)
                .containsExactly(BudgetItemSubcategory.of("Auto"), BudgetItemSubcategory.of("Miete"));
        assertThat(living.groups().getFirst().total()).isEqualTo(money("1750.00"));
        assertThat(living.groups().getFirst().items()).containsExactly(fuel, tax, maintenance);
        assertThat(living.groups().getLast().total()).isEqualTo(money("9600.00"));
    }

    /** UK-9, UK-10, UK-12 */
    @Test
    void listsTheIncomesByGroupAndTheCategoriesWithExpensesInTheOrderOfTheRule() {
        OverviewItem salary = income("Gehalt", "Gehalt", "3000.00");
        OverviewItem savings = expense("ETF", ExpenseCategory.SPAREN_INVESTIEREN, "Investieren", "300.00");
        OverviewItem rent = expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", "1000.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(savings, salary, rent),
                Map.of());

        assertThat(overview.incomeGroups()).singleElement().satisfies(group -> {
            assertThat(group.subcategory()).isEqualTo(BudgetItemSubcategory.of("Gehalt"));
            assertThat(group.total()).isEqualTo(money("3000.00"));
            assertThat(group.items()).containsExactly(salary);
        });
        assertThat(overview.expenseCategories()).extracting(OverviewCategoryItems::category)
                .containsExactly(ExpenseCategory.LEBENSUNTERHALT, ExpenseCategory.SPAREN_INVESTIEREN);
        // Only the recorded expenses, never the surplus of 1700.00 the breakdowns add (AW-10, UK-12)
        assertThat(overview.expenseCategories().getLast().total()).isEqualTo(money("300.00"));
    }

    /** UK-1, UK-8, UK-16 */
    @Test
    void itemsWithoutSubcategoryAndThoseOfOtherShareTheLastGroup() {
        OverviewItem none = expense("Kleinkram", ExpenseCategory.FREIZEIT_SPASS, null, "10.00");
        OverviewItem other = expense("Diverses", ExpenseCategory.FREIZEIT_SPASS, "Sonstiges", "20.00");
        OverviewItem hobby = expense("Gitarre", ExpenseCategory.FREIZEIT_SPASS, "Hobbys", "30.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(none, other, hobby),
                Map.of());

        List<OverviewItemGroup> groups = overview.expenseCategories().getFirst().groups();
        assertThat(groups).extracting(OverviewItemGroup::subcategory)
                .containsExactly(BudgetItemSubcategory.of("Hobbys"), BudgetItemSubcategory.OTHER);
        assertThat(groups.getLast().items()).containsExactly(none, other);
        assertThat(groups.getLast().total()).isEqualTo(money("30.00"));
    }

    /** UK-3 */
    @Test
    void sameSubcategoryInAnotherCategoryIsAGroupOfItsOwn() {
        OverviewItem maintenance = expense("Wartung", ExpenseCategory.LEBENSUNTERHALT, "Auto", "400.00");
        OverviewItem trip = expense("Ausflug", ExpenseCategory.FREIZEIT_SPASS, "Auto", "80.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(trip, maintenance),
                Map.of());

        assertThat(overview.expenseCategories()).extracting(category -> category.groups().getFirst().items())
                .containsExactly(List.of(maintenance), List.of(trip));
    }

    /** UK-14 */
    @Test
    void groupsFollowTheArrangedOrderOfTheirSection() {
        OverviewItem car = expense("Wartung", ExpenseCategory.LEBENSUNTERHALT, "Auto", "400.00");
        OverviewItem rent = expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", "9600.00");
        OverviewItem holiday = expense("Urlaub", ExpenseCategory.LEBENSUNTERHALT, "Urlaub", "1000.00");
        SubcategoryOrder arranged = new SubcategoryOrder(
                List.of(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto")));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(car, rent, holiday),
                Map.of(ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT), arranged));

        assertThat(overview.expenseCategories().getFirst().groups()).extracting(OverviewItemGroup::subcategory)
                .containsExactly(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto"),
                        BudgetItemSubcategory.of("Urlaub"));
    }

    /** MJ-9 */
    @Test
    void groupsAndCategoriesShowTheirYearlySumDividedByTwelvePerMonth() {
        OverviewItem first = expense("Zeitung", ExpenseCategory.FREIZEIT_SPASS, "Abos", "100.00");
        OverviewItem second = expense("Podcast", ExpenseCategory.FREIZEIT_SPASS, "Abos", "100.00");
        OverviewItem third = expense("Magazin", ExpenseCategory.FREIZEIT_SPASS, "Abos", "100.00");

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(first, second, third),
                Map.of());

        OverviewCategoryItems leisure = overview.expenseCategories().getFirst();
        assertThat(leisure.groups().getFirst().items())
                .allSatisfy(item -> assertThat(item.amount().monthly()).isEqualTo(money("8.33")));
        assertThat(leisure.groups().getFirst().total()).isEqualTo(money("300.00"));
        assertThat(leisure.groups().getFirst().monthlyTotal()).isEqualTo(money("25.00"));
        assertThat(leisure.monthlyTotal()).isEqualTo(money("25.00"));
    }

    /** MJ-6, MJ-8 */
    @Test
    void sumsUpTheYearlyAmountsOfMonthlyItems() {
        OverviewItem rent = new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of("Miete"),
                ItemAmount.monthly(money("850.00")), ItemType.EXPENSE, ExpenseCategory.LEBENSUNTERHALT, null, null);

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(rent), Map.of());

        assertThat(overview.expenseCategories().getFirst().total()).isEqualTo(money("10200.00"));
        assertThat(overview.expenseCategories().getFirst().monthlyTotal()).isEqualTo(money("850.00"));
    }

    @Test
    void anEmptyPlanHasNoGroups() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, List.of(), Map.of());

        assertThat(overview.incomeGroups()).isEmpty();
        assertThat(overview.expenseCategories()).isEmpty();
    }

    private static OverviewItem income(String description, String subcategory, String amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description),
                ItemAmount.yearly(money(amount)), ItemType.INCOME, null, BudgetItemSubcategory.ofNullable(subcategory),
                null);
    }

    private static OverviewItem expense(String description, ExpenseCategory category, String subcategory,
                                        String amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description),
                ItemAmount.yearly(money(amount)), ItemType.EXPENSE, category, BudgetItemSubcategory.ofNullable(subcategory),
                null);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
