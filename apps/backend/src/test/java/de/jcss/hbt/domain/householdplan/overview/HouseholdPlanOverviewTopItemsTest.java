package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
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

/** The top 5 lists of the overview: saving potential and expenses (TP-1 to TP-4). */
class HouseholdPlanOverviewTopItemsTest {

    private static final HouseholdPlanId PLAN_ID = HouseholdPlanId.generate();
    private static final Year YEAR = Year.of(2026);

    /** TP-1, TP-3, TP-4 */
    @Test
    void listsTheExpensesWithSavingPotentialTheLargestFirst() {
        List<OverviewItem> items = List.of(
                expense("Streaming", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.monthly(money("15.00")), "5.00"),
                expense("Fitnessstudio", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.yearly(money("360.00")), "30.00"),
                expense("Miete", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("1000.00")), null),
                income("Gehalt", ItemAmount.monthly(money("3000.00"))));

        assertThat(overviewOf(items).topSavingPotentials())
                .extracting(row -> row.item().description().value(), TopItem::monthly, TopItem::annual)
                .containsExactly(
                        tuple("Fitnessstudio", money("30.00"), money("360.00")),
                        tuple("Streaming", money("5.00"), money("60.00")));
    }

    /** TP-1 */
    @Test
    void takesTheSavingPotentialOfAllCategories() {
        List<OverviewItem> items = List.of(
                expense("ETF-Sparplan", ExpenseCategory.SPAREN_INVESTIEREN, ItemAmount.monthly(money("500.00")), "100.00"));

        assertThat(overviewOf(items).topSavingPotentials()).singleElement()
                .satisfies(row -> assertThat(row.item().description().value()).isEqualTo("ETF-Sparplan"));
    }

    /** TP-2, TP-4 */
    @Test
    void listsAtMostFiveExpensesOfLivingCostsAndLeisureByMonthlyAmount() {
        List<OverviewItem> items = List.of(
                expense("Miete", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("1000.00")), null),
                expense("Lebensmittel", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("400.00")), null),
                expense("Strom", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("80.00")), null),
                expense("Restaurant", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.monthly(money("150.00")), null),
                expense("Kino", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.monthly(money("20.00")), null),
                expense("Urlaub", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.yearly(money("2400.00")), null),
                expense("Tanken", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("120.00")), null),
                expense("ETF-Sparplan", ExpenseCategory.SPAREN_INVESTIEREN, ItemAmount.monthly(money("500.00")), null),
                income("Gehalt", ItemAmount.monthly(money("3000.00"))));

        assertThat(overviewOf(items).topExpenses())
                .extracting(row -> row.item().description().value())
                .containsExactly("Miete", "Lebensmittel", "Urlaub", "Restaurant", "Tanken");
    }

    /** TP-3 */
    @Test
    void ordersAYearlyExpenseByItsMonthlyAmountAndShowsItsYearlyAmount() {
        List<OverviewItem> items = List.of(
                expense("Handy", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.monthly(money("40.00")), null),
                expense("Kfz-Versicherung", ExpenseCategory.LEBENSUNTERHALT, ItemAmount.yearly(money("600.00")), null));

        assertThat(overviewOf(items).topExpenses())
                .extracting(row -> row.item().description().value(), TopItem::monthly, TopItem::annual)
                .containsExactly(
                        tuple("Kfz-Versicherung", money("50.00"), money("600.00")),
                        tuple("Handy", money("40.00"), money("480.00")));
    }

    /** TP-4 */
    @Test
    void ordersEqualAmountsAlphabetically() {
        List<OverviewItem> items = List.of(
                expense("Kino", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.monthly(money("20.00")), "5.00"),
                expense("bücher", ExpenseCategory.FREIZEIT_SPASS, ItemAmount.monthly(money("20.00")), "5.00"));

        HouseholdPlanOverview overview = overviewOf(items);

        assertThat(overview.topExpenses()).extracting(row -> row.item().description().value())
                .containsExactly("bücher", "Kino");
        assertThat(overview.topSavingPotentials()).extracting(row -> row.item().description().value())
                .containsExactly("bücher", "Kino");
    }

    /** TP-1, TP-2 */
    @Test
    void anEmptyPlanHasEmptyLists() {
        HouseholdPlanOverview overview = overviewOf(List.of());

        assertThat(overview.topSavingPotentials()).isEmpty();
        assertThat(overview.topExpenses()).isEmpty();
    }

    private static HouseholdPlanOverview overviewOf(List<OverviewItem> items) {
        return HouseholdPlanOverview.of(PLAN_ID, YEAR, items, Map.of());
    }

    private static OverviewItem expense(String description, ExpenseCategory category, ItemAmount amount,
                                        String savingPotential) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description), amount,
                ItemType.EXPENSE, category, BudgetItemSubcategory.ofNullable(null), null, false,
                savingPotential == null ? null : SavingPotential.monthly(money(savingPotential)));
    }

    private static OverviewItem income(String description, ItemAmount amount) {
        return new OverviewItem(BudgetItemId.generate(), BudgetItemDescription.of(description), amount,
                ItemType.INCOME, null, null, null);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
