package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
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

    @Test
    void reconstituteRecordsNoEvents() {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), salary, amount);

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
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), salary, amount);

        assertThatThrownBy(() -> HouseholdPlan.reconstitute(planId, OWNER, YEAR, List.of(item, item)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("twice");
    }

    @Test
    void summarizesItsItems() {
        HouseholdPlan plan = emptyPlan();
        plan.addIncome(salary, Money.of(new BigDecimal("1000.00")));
        plan.addExpense(rent, Money.of(new BigDecimal("600.00")), ExpenseCategory.LEBENSUNTERHALT);

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

        BudgetItemId incomeId = plan.addIncome(salary, amount);
        BudgetItemId expenseId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT);

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

        assertThatThrownBy(() -> plan.addExpense(rent, amount, null))
                .isInstanceOf(InvalidBudgetItemException.class);

        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void removesItemAndRecordsIt() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addIncome(salary, amount);
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
        BudgetItemId first = plan.addIncome(salary, amount);
        BudgetItemId changedId = plan.addExpense(rent, Money.of(new BigDecimal("850.00")), ExpenseCategory.LEBENSUNTERHALT);
        BudgetItemId last = plan.addIncome(salary, amount);
        plan.pullDomainEvents();

        plan.changeItem(changedId, BudgetItemDescription.of("Warmmiete"), Money.of(new BigDecimal("900.00")),
                ExpenseCategory.SPAREN_INVESTIEREN);

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
        BudgetItemId itemId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT);
        plan.pullDomainEvents();

        plan.changeItem(itemId, rent, Money.of(new BigDecimal("100")), ExpenseCategory.LEBENSUNTERHALT);

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PB-2, PB-3 */
    @Test
    void invalidChangeLeavesItemUnchangedAndRecordsNothing() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId incomeId = plan.addIncome(salary, amount);
        BudgetItemId expenseId = plan.addExpense(rent, amount, ExpenseCategory.LEBENSUNTERHALT);
        List<BudgetItem> before = List.copyOf(plan.items());
        plan.pullDomainEvents();

        assertThatThrownBy(() -> plan.changeItem(incomeId, salary, amount, ExpenseCategory.LEBENSUNTERHALT))
                .isInstanceOf(InvalidBudgetItemException.class);
        assertThatThrownBy(() -> plan.changeItem(expenseId, rent, amount, null))
                .isInstanceOf(InvalidBudgetItemException.class);
        assertThatThrownBy(() -> plan.changeItem(expenseId, rent, Money.ZERO, ExpenseCategory.LEBENSUNTERHALT))
                .isInstanceOf(InvalidBudgetItemException.class);

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

        assertThatThrownBy(() -> plan.changeItem(BudgetItemId.generate(), salary, amount, null))
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

        assertThatThrownBy(() -> plan.items().add(BudgetItem.income(BudgetItemId.generate(), salary, amount)))
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
