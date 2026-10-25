package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** The saving potential of the overview per subcategory (SU-1 to SU-4) and per group (SU-10, SU-12). */
class HouseholdPlanOverviewSavingPotentialTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final Year YEAR = Year.of(2026);

    /** SU-1, SU-2, SU-4 */
    @Test
    void sumsUpTheSavingPotentialsPerSubcategoryTheLargestFirst() {
        List<OverviewItem> items = List.of(
                expense("Streaming", ExpenseCategory.FREIZEIT_SPASS, "Abonnements", "5.00"),
                expense("Zeitung", ExpenseCategory.FREIZEIT_SPASS, "abonnements", "10.00"),
                expense("Fitnessstudio", ExpenseCategory.FREIZEIT_SPASS, "Hobbys", "30.00"));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.savingPotentialsBySubcategory())
                .extracting(sum -> sum.subcategory().value(), SubcategorySavingPotential::monthly,
                        SubcategorySavingPotential::annual)
                .containsExactly(
                        tuple("Hobbys", money("30.00"), money("360.00")),
                        tuple("Abonnements", money("15.00"), money("180.00")));
    }

    /** SU-2 */
    @Test
    void combinesASubcategoryAcrossCategoriesInTheSpellingTheEntriesShowFirst() {
        List<OverviewItem> items = List.of(
                expense("Ausflüge", ExpenseCategory.FREIZEIT_SPASS, "auto", "10.00"),
                expense("Tanken", ExpenseCategory.LEBENSUNTERHALT, "Auto", "10.00"));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.savingPotentialsBySubcategory()).singleElement().satisfies(sum -> {
            assertThat(sum.subcategory().value()).isEqualTo("Auto");
            assertThat(sum.monthly()).isEqualTo(money("20.00"));
        });
    }

    /** SU-2, SU-3 */
    @Test
    void listsExpensesWithoutSubcategoryAsOtherAndLeavesOutSubcategoriesWithoutSavingPotential() {
        List<OverviewItem> items = List.of(
                expense("Kleinkram", ExpenseCategory.FREIZEIT_SPASS, null, "4.00"),
                expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", null));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.savingPotentialsBySubcategory()).singleElement().satisfies(sum -> {
            assertThat(sum.subcategory()).isEqualTo(BudgetItemSubcategory.OTHER);
            assertThat(sum.monthly()).isEqualTo(money("4.00"));
        });
    }

    /** SU-3 */
    @Test
    void theSumsAddUpToTheSavingPotentialOfThePlan() {
        List<OverviewItem> items = List.of(
                expense("Streaming", ExpenseCategory.FREIZEIT_SPASS, "Abonnements", "5.00"),
                expense("Tanken", ExpenseCategory.LEBENSUNTERHALT, "Auto", "12.50"),
                expense("Kleinkram", ExpenseCategory.FREIZEIT_SPASS, null, "4.00"));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.savingPotentialsBySubcategory().stream()
                .map(SubcategorySavingPotential::monthly)
                .reduce(Money.ZERO, Money::add))
                .isEqualTo(overview.monthlySavingPotential());
    }

    /** SU-4 */
    @Test
    void equalSumsAreOrderedAlphabetically() {
        List<OverviewItem> items = List.of(
                expense("Zeitung", ExpenseCategory.FREIZEIT_SPASS, "Zeitschriften", "5.00"),
                expense("Tanken", ExpenseCategory.LEBENSUNTERHALT, "auto", "5.00"));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.savingPotentialsBySubcategory()).extracting(sum -> sum.subcategory().value())
                .containsExactly("auto", "Zeitschriften");
    }

    /** SU-10 */
    @Test
    void eachGroupSumsUpTheSavingPotentialsOfItsOwnItems() {
        List<OverviewItem> items = List.of(
                expense("Streaming", ExpenseCategory.FREIZEIT_SPASS, "Abonnements", "5.00"),
                expense("Zeitung", ExpenseCategory.FREIZEIT_SPASS, "Abonnements", "10.00"),
                expense("Ausflüge", ExpenseCategory.FREIZEIT_SPASS, "Auto", "10.00"),
                expense("Tanken", ExpenseCategory.LEBENSUNTERHALT, "Auto", "12.50"));

        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());

        assertThat(overview.expenseCategories())
                .flatExtracting(OverviewCategoryItems::groups)
                .extracting(group -> group.subcategory().value(), OverviewItemGroup::monthlySavingPotential)
                .containsExactly(
                        tuple("Auto", money("12.50")),
                        tuple("Abonnements", money("15.00")),
                        tuple("Auto", money("10.00")));
    }

    /** SU-12 */
    @Test
    void aGroupWithoutSavingPotentialHasZero() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR,
                List.of(expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", null)), Map.of());

        assertThat(overview.expenseCategories().getFirst().groups().getFirst().monthlySavingPotential())
                .isEqualTo(Money.ZERO);
    }

    @Test
    void aPlanWithoutSavingPotentialHasNoSums() {
        HouseholdPlanOverview overview = HouseholdPlanOverview.of(PLAN_ID, YEAR,
                List.of(expense("Miete", ExpenseCategory.LEBENSUNTERHALT, "Miete", null)), Map.of());

        assertThat(overview.savingPotentialsBySubcategory()).isEmpty();
    }

    /** @param savingPotential monthly, {@code null} for none */
    private static OverviewItem expense(String description, ExpenseCategory category, String subcategory,
                                        String savingPotential) {
        ItemMarks marks = savingPotential == null
                ? ItemMarks.NONE
                : new ItemMarks(false, SavingPotential.monthly(money(savingPotential)));
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description),
                ItemAmount.monthly(money("100.00")), ItemType.EXPENSE, category,
                BudgetItemSubcategory.ofNullable(subcategory), null, marks);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
