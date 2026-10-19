package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemChanged;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HouseholdPlanTest {

    private static final Year YEAR = Year.of(2026);
    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());

    private final HouseholdPlanId planId = HouseholdPlanId.generate();
    private final Money amount = Money.of(new BigDecimal("100.00"));
    private final BudgetItemDescription salary = BudgetItemDescription.of("Gehalt");
    private final BudgetItemDescription rent = BudgetItemDescription.of("Miete");

    @Test
    void createStartsAnEmptyPlanAndRecordsIt() {
        HouseholdPlan plan = HouseholdPlan.create(planId, OWNER, YEAR);

        assertThat(plan.id()).isEqualTo(planId);
        assertThat(plan.owner()).isEqualTo(OWNER);
        assertThat(plan.year()).isEqualTo(YEAR);
        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new HouseholdPlanCreated(planId, OWNER, YEAR, Instant.now()));
    }

    /** HP-1 */
    @Test
    void createAcceptsTheYears1900To2200() {
        assertThat(HouseholdPlan.create(planId, OWNER, Year.of(1900)).year()).isEqualTo(Year.of(1900));
        assertThat(HouseholdPlan.create(planId, OWNER, Year.of(2200)).year()).isEqualTo(Year.of(2200));
    }

    /** HP-1 */
    @Test
    void createRejectsYearsBefore1900OrAfter2200() {
        assertThatThrownBy(() -> HouseholdPlan.create(planId, OWNER, Year.of(1899)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("year must be between 1900 and 2200, but was 1899");
        assertThatThrownBy(() -> HouseholdPlan.create(planId, OWNER, Year.of(2201)))
                .isInstanceOf(DomainValidationException.class);
    }

    /** HP-1: stored plans of other years stay readable */
    @Test
    void reconstituteAcceptsYearsOutsideTheRangeForNewPlans() {
        HouseholdPlan plan = HouseholdPlan.reconstitute(planId, OWNER, Year.of(1850), List.of());

        assertThat(plan.year()).isEqualTo(Year.of(1850));
    }

    @Test
    void reconstituteRecordsNoEvents() {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), salary, amount, null);

        HouseholdPlan plan = HouseholdPlan.reconstitute(planId, OWNER, YEAR, List.of(item));

        assertThat(plan.items()).containsExactly(item);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void eventsRecordWhenTheChangeWasMade() {
        Instant before = Instant.now();

        HouseholdPlan plan = HouseholdPlan.create(planId, OWNER, YEAR);

        assertThat(plan.pullDomainEvents()).singleElement()
                .isInstanceOfSatisfying(HouseholdPlanCreated.class,
                        event -> assertThat(event.timestamp()).isBetween(before, Instant.now()));
    }

    @Test
    void reconstituteRejectsMissingItems() {
        assertThatThrownBy(() -> HouseholdPlan.reconstitute(planId, OWNER, YEAR, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HouseholdPlan.reconstitute(planId, OWNER, YEAR, Arrays.asList((BudgetItem) null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reconstituteRejectsTheSameItemTwice() {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), salary, amount, null);

        assertThatThrownBy(() -> HouseholdPlan.reconstitute(planId, OWNER, YEAR, List.of(item, item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("twice");
    }

    @Test
    void summarizesItsItems() {
        HouseholdPlan plan = emptyPlan();
        plan.addIncome(salary, Money.of(new BigDecimal("1000.00")), null);
        plan.addExpense(rent, Money.of(new BigDecimal("600.00")), ExpenseCategory.LEBENSUNTERHALT, null);

        BudgetSummary summary = plan.summary();

        assertThat(summary.totalIncome()).isEqualTo(Money.of(new BigDecimal("1000.00")));
        assertThat(summary.expensesIn(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(Money.of(new BigDecimal("600.00")));
        assertThat(summary.isTargetMet(ExpenseCategory.LEBENSUNTERHALT)).isFalse();
    }

    @Test
    void summaryOfAnEmptyPlanIsEmpty() {
        assertThat(emptyPlan().summary()).isEqualTo(BudgetSummary.EMPTY);
    }

    @Test
    void addsIncomeAndExpenseInOrderAndRecordsEachChange() {
        HouseholdPlan plan = emptyPlan();

        BudgetItemId incomeId = plan.addIncome(salary, amount, null);
        BudgetItemId expenseId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(incomeId, expenseId);
        assertThat(plan.items().get(1).category()).contains(ExpenseCategory.LEBENSUNTERHALT);
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(
                        new IncomeAdded(planId, incomeId, Instant.now()),
                        new ExpenseAdded(planId, expenseId, Instant.now()));
    }

    @Test
    void invalidItemIsNeitherAddedNorRecorded() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.addExpense(rent, amount, null, null))
                .isInstanceOf(DomainValidationException.class);

        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void removesItemAndRecordsIt() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addIncome(salary, amount, null);
        plan.pullDomainEvents();

        plan.removeItem(itemId);

        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemRemoved(planId, itemId, Instant.now()));
    }

    @Test
    void removingAnItemOfAnotherPlanFails() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.removeItem(BudgetItemId.generate()))
                .isInstanceOf(BudgetItemNotFoundException.class);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PB-1, PB-4, PB-5, PB-7 */
    @Test
    void changesItemInPlaceAndRecordsIt() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId first = plan.addIncome(salary, amount, null);
        BudgetItemId changedId = plan.addExpense(rent, Money.of(new BigDecimal("850.00")), ExpenseCategory.LEBENSUNTERHALT, null);
        BudgetItemId last = plan.addIncome(salary, amount, null);
        plan.pullDomainEvents();

        plan.changeItem(changedId, BudgetItemDescription.of("Warmmiete"), Money.of(new BigDecimal("900.00")),
                ExpenseCategory.SPAREN_INVESTIEREN, null);

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(first, changedId, last);
        BudgetItem changed = plan.items().get(1);
        assertThat(changed.description()).isEqualTo(BudgetItemDescription.of("Warmmiete"));
        assertThat(changed.amount()).isEqualTo(Money.of(new BigDecimal("900.00")));
        assertThat(changed.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(changed.category()).contains(ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(plan.summary().expensesIn(ExpenseCategory.LEBENSUNTERHALT)).isEqualTo(Money.ZERO);
        assertThat(plan.summary().expensesIn(ExpenseCategory.SPAREN_INVESTIEREN))
                .isEqualTo(Money.of(new BigDecimal("900.00")));
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemChanged(planId, changedId, Instant.now()));
    }

    /** PB-5 */
    @Test
    void changingAnItemToItsCurrentValuesRecordsNothing() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);
        plan.pullDomainEvents();

        plan.changeItem(itemId, rent, Money.of(new BigDecimal("100")), ExpenseCategory.LEBENSUNTERHALT, null);

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PN-1, PN-4 */
    @Test
    void keepsTheNoteOfNewItems() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemNote note = new BudgetItemNote("Warmmiete inkl. Nebenkosten");

        plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, note);
        plan.addIncome(salary, amount, null);

        assertThat(plan.items()).extracting(BudgetItem::note).containsExactly(Optional.of(note), Optional.empty());
    }

    /** PN-4, PN-5 */
    @Test
    void addingChangingOrRemovingTheNoteIsAChange() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);
        plan.pullDomainEvents();

        plan.changeItem(itemId, rent, amount, ExpenseCategory.LEBENSUNTERHALT, new BudgetItemNote("Warmmiete"));
        assertThat(plan.items().getFirst().note()).contains(new BudgetItemNote("Warmmiete"));
        plan.changeItem(itemId, rent, amount, ExpenseCategory.LEBENSUNTERHALT, new BudgetItemNote("Kaltmiete"));
        assertThat(plan.items().getFirst().note()).contains(new BudgetItemNote("Kaltmiete"));
        plan.changeItem(itemId, rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);
        assertThat(plan.items().getFirst().note()).isEmpty();

        assertThat(plan.pullDomainEvents()).hasSize(3).allMatch(BudgetItemChanged.class::isInstance);
    }

    /** PN-5 */
    @Test
    void keepingTheSameNoteRecordsNothing() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addIncome(salary, amount, new BudgetItemNote("netto"));
        plan.pullDomainEvents();

        plan.changeItem(itemId, salary, amount, null, new BudgetItemNote(" netto "));

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PN-6 */
    @Test
    void theNoteDoesNotAffectTheSummary() {
        HouseholdPlan withNote = emptyPlan();
        withNote.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, new BudgetItemNote("Warmmiete"));
        HouseholdPlan withoutNote = emptyPlan();
        withoutNote.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);

        assertThat(withNote.summary()).isEqualTo(withoutNote.summary());
    }

    /** PB-2, PB-3 */
    @Test
    void invalidChangeLeavesItemUnchangedAndRecordsNothing() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId incomeId = plan.addIncome(salary, amount, null);
        BudgetItemId expenseId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT, null);
        List<BudgetItem> before = List.copyOf(plan.items());
        plan.pullDomainEvents();

        assertThatThrownBy(() -> plan.changeItem(incomeId, salary, amount, ExpenseCategory.LEBENSUNTERHALT, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.changeItem(expenseId, rent, amount, null, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> plan.changeItem(expenseId, rent, Money.ZERO, ExpenseCategory.LEBENSUNTERHALT, null))
                .isInstanceOf(DomainValidationException.class);

        assertThat(plan.items()).hasSize(2);
        for (int i = 0; i < before.size(); i++) {
            assertThat(plan.items().get(i).hasSameValuesAs(before.get(i))).isTrue();
        }
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PB-6 */
    @Test
    void changingAnItemOfAnotherPlanFails() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.changeItem(BudgetItemId.generate(), salary, amount, null, null))
                .isInstanceOf(BudgetItemNotFoundException.class);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void belongsOnlyToItsOwner() {
        HouseholdPlan plan = emptyPlan();

        assertThat(plan.isOwnedBy(OWNER)).isTrue();
        assertThat(plan.isOwnedBy(OwnerId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void pullingEventsForgetsThem() {
        HouseholdPlan plan = HouseholdPlan.create(planId, OWNER, YEAR);

        assertThat(plan.pullDomainEvents()).hasSize(1);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void itemsCannotBeChangedFromOutside() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.items().add(BudgetItem.income(BudgetItemId.generate(), salary, amount, null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        HouseholdPlan first = HouseholdPlan.reconstitute(planId, OWNER, YEAR, List.of());
        HouseholdPlan second = HouseholdPlan.reconstitute(planId, OWNER, Year.of(2027), List.of());

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    private HouseholdPlan emptyPlan() {
        return HouseholdPlan.reconstitute(planId, OWNER, YEAR, List.of());
    }
}
