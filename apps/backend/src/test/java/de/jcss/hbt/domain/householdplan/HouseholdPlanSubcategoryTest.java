package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemChanged;
import de.jcss.hbt.domain.householdplan.event.BudgetItemsReordered;
import de.jcss.hbt.domain.householdplan.event.SubcategoriesReordered;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Subcategories of the items of a plan and the order they are shown in (UK). */
class HouseholdPlanSubcategoryTest {

    private static final ItemSection LIVING = ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT);
    private static final BudgetItemSubcategory CAR = BudgetItemSubcategory.of("Auto");
    private static final BudgetItemSubcategory RENT = BudgetItemSubcategory.of("Miete");

    private final HouseholdPlanId planId = HouseholdPlanId.generate();
    private final HouseholdPlan plan = HouseholdPlan.reconstitute(planId, OwnerId.of(UUID.randomUUID()),
            Year.of(2026), List.of(), Map.of());
    private final ItemAmount amount = ItemAmount.yearly(Money.of(new BigDecimal("100.00")));

    /** UK-1, UK-4 */
    @Test
    void itemsKeepTheirSubcategory() {
        BudgetItemId incomeId = plan.addIncome(description("Gehalt"), amount, BudgetItemSubcategory.of("Gehalt"), null);
        BudgetItemId expenseId = expense("Wartung", CAR);

        assertThat(item(incomeId).subcategory()).contains(BudgetItemSubcategory.of("Gehalt"));
        assertThat(item(expenseId).subcategory()).contains(CAR);
        assertThat(item(expenseId).group()).isEqualTo(CAR);
    }

    /** UK-1 */
    @Test
    void itemsWithoutSubcategoryBelongToOther() {
        BudgetItemId itemId = expense("Kleinkram", null);

        assertThat(item(itemId).subcategory()).isEmpty();
        assertThat(item(itemId).group()).isEqualTo(BudgetItemSubcategory.OTHER);
    }

    /** UK-5 */
    @Test
    void newItemTakesTheSpellingOfItsGroup() {
        expense("Wartung", CAR);
        BudgetItemId itemId = expense("Steuer", BudgetItemSubcategory.of("auto"));
        BudgetItemId otherId = expense("Kleinkram", BudgetItemSubcategory.of("SONSTIGES"));

        assertThat(item(itemId).subcategory()).contains(CAR);
        assertThat(item(otherId).subcategory()).contains(BudgetItemSubcategory.OTHER);
    }

    /** UK-5 */
    @Test
    void singleItemMayRespellItsGroup() {
        BudgetItemId itemId = expense("Wartung", CAR);

        plan.changeItem(itemId, description("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.of("AUTO"), null);

        assertThat(item(itemId).subcategory()).contains(BudgetItemSubcategory.of("AUTO"));
    }

    /** UK-3 */
    @Test
    void subcategoryDoesNotAffectTheSummary() {
        HouseholdPlan other = HouseholdPlan.reconstitute(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()),
                Year.of(2026), List.of(), Map.of());
        expense("Wartung", CAR);
        other.addExpense(description("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT, null, null);

        assertThat(plan.summary()).isEqualTo(other.summary());
    }

    /** UK-4 */
    @Test
    void changingOnlyTheSubcategoryIsAChange() {
        BudgetItemId itemId = expense("Wartung", CAR);
        plan.pullDomainEvents();

        plan.changeItem(itemId, description("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.of("Mobilität"), null);

        assertThat(item(itemId).subcategory()).contains(BudgetItemSubcategory.of("Mobilität"));
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemChanged(planId, itemId, Instant.now()));
    }

    /** EG-2 */
    @Test
    void itemMovedToAnotherGroupGoesToItsBottom() {
        BudgetItemId rent = expense("Kaltmiete", RENT);
        BudgetItemId moved = expense("Wartung", CAR);
        BudgetItemId newest = expense("Nebenkosten", RENT);

        plan.changeItem(moved, description("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT, RENT, null);

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(rent, newest, moved);
    }

    /** UK-14 */
    @Test
    void reordersTheItemsOfAGroupAndRecordsIt() {
        BudgetItemId maintenance = expense("Wartung", CAR);
        BudgetItemId rent = expense("Kaltmiete", RENT);
        BudgetItemId tax = expense("Steuer", CAR);
        BudgetItemId fuel = expense("Sprit", CAR);
        plan.pullDomainEvents();

        plan.reorderItems(List.of(maintenance, fuel, tax));

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(maintenance, rent, fuel, tax);
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemsReordered(planId, Instant.now()));
    }

    /** UK-14 */
    @Test
    void keepingTheOrderOfTheItemsRecordsNothing() {
        BudgetItemId first = expense("Wartung", CAR);
        BudgetItemId second = expense("Steuer", CAR);
        plan.pullDomainEvents();

        plan.reorderItems(List.of(first, second));

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** UK-14 */
    @Test
    void reorderingItemsMustNameExactlyTheItemsOfOneGroup() {
        BudgetItemId maintenance = expense("Wartung", CAR);
        BudgetItemId tax = expense("Steuer", CAR);
        BudgetItemId rent = expense("Kaltmiete", RENT);
        BudgetItemId otherCategory = plan.addExpense(description("Ausflug"), amount, ExpenseCategory.FREIZEIT_SPASS,
                CAR, null);
        plan.pullDomainEvents();
        List<BudgetItem> before = List.copyOf(plan.items());

        assertThatThrownBy(() -> plan.reorderItems(List.of(tax)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderItems(List.of(tax, maintenance, rent)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderItems(List.of(tax, otherCategory)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderItems(List.of(tax, tax)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderItems(List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderItems(List.of(tax, BudgetItemId.generate())))
                .isInstanceOf(BudgetItemNotFoundException.class);

        assertThat(plan.items()).containsExactlyElementsOf(before);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** UK-14, UK-15 */
    @Test
    void reordersTheGroupsOfASectionAndRecordsIt() {
        expense("Wartung", CAR);
        expense("Kaltmiete", RENT);
        plan.pullDomainEvents();

        plan.reorderSubcategories(LIVING, List.of(BudgetItemSubcategory.of("miete"), CAR));

        assertThat(plan.subcategoryOrderOf(LIVING).arrange(List.of(CAR, RENT))).containsExactly(RENT, CAR);
        assertThat(plan.subcategoryOrderOf(ItemSection.INCOME)).isEqualTo(SubcategoryOrder.NONE);
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new SubcategoriesReordered(planId, Instant.now()));
    }

    /** UK-16 */
    @Test
    void keepingTheAlphabeticalOrderOfTheGroupsRecordsNothing() {
        expense("Kaltmiete", RENT);
        expense("Wartung", CAR);
        plan.pullDomainEvents();

        plan.reorderSubcategories(LIVING, List.of(CAR, RENT));

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** UK-14 */
    @Test
    void reorderingGroupsMustNameExactlyTheGroupsOfTheSection() {
        expense("Wartung", CAR);
        expense("Kaltmiete", RENT);
        expense("Kleinkram", null);
        plan.pullDomainEvents();

        assertThatThrownBy(() -> plan.reorderSubcategories(LIVING, List.of(CAR, RENT)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderSubcategories(LIVING,
                List.of(CAR, RENT, BudgetItemSubcategory.OTHER, BudgetItemSubcategory.of("Urlaub"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.reorderSubcategories(ItemSection.expenses(ExpenseCategory.FREIZEIT_SPASS),
                List.of(CAR)))
                .isInstanceOf(DomainValidationException.class);

        assertThat(plan.subcategoryOrders()).isEmpty();
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** UK-18 */
    @Test
    void vanishedGroupIsForgottenSoItCountsAsNewWhenItComesBack() {
        BudgetItemId car = expense("Wartung", CAR);
        expense("Kaltmiete", RENT);
        expense("Kleinkram", null);
        plan.reorderSubcategories(LIVING, List.of(CAR, BudgetItemSubcategory.OTHER, RENT));

        plan.removeItem(car);

        assertThat(plan.subcategoryOrderOf(LIVING))
                .isEqualTo(new SubcategoryOrder(List.of(BudgetItemSubcategory.OTHER, RENT)));
    }

    /** UK-18 */
    @Test
    void groupLeftByItsLastItemIsForgotten() {
        BudgetItemId car = expense("Wartung", CAR);
        expense("Kaltmiete", RENT);
        plan.reorderSubcategories(LIVING, List.of(RENT, CAR));

        plan.changeItem(car, description("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT, RENT, null);

        assertThat(plan.subcategoryOrderOf(LIVING)).isEqualTo(new SubcategoryOrder(List.of(RENT)));
    }

    private BudgetItemId expense(String description, BudgetItemSubcategory subcategory) {
        return plan.addExpense(description(description), amount, ExpenseCategory.LEBENSUNTERHALT, subcategory, null);
    }

    private BudgetItem item(BudgetItemId itemId) {
        return plan.items().stream().filter(item -> item.id().equals(itemId)).findFirst().orElseThrow();
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }
}
