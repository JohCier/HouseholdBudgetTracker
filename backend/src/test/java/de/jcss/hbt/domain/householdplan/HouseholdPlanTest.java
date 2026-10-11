package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HouseholdPlanTest {

    private static final Year YEAR = Year.of(2026);

    private final HouseholdPlanId planId = HouseholdPlanId.generate();
    private final Money amount = Money.of(new BigDecimal("100.00"));

    @Test
    void createStartsAnEmptyPlanAndRecordsIt() {
        HouseholdPlan plan = HouseholdPlan.create(planId, YEAR);

        assertThat(plan.id()).isEqualTo(planId);
        assertThat(plan.year()).isEqualTo(YEAR);
        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents()).containsExactly(new HouseholdPlanCreated(planId, YEAR));
    }

    @Test
    void reconstituteRecordsNoEvents() {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), "Gehalt", amount);

        HouseholdPlan plan = HouseholdPlan.reconstitute(planId, YEAR, List.of(item));

        assertThat(plan.items()).containsExactly(item);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void addsIncomeAndExpenseInOrderAndRecordsEachChange() {
        HouseholdPlan plan = emptyPlan();

        BudgetItemId incomeId = plan.addIncome("Gehalt", amount);
        BudgetItemId expenseId = plan.addExpense("Miete", amount, ExpenseCategory.LEBENSUNTERHALT);

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(incomeId, expenseId);
        assertThat(plan.items().get(1).category()).contains(ExpenseCategory.LEBENSUNTERHALT);
        assertThat(plan.pullDomainEvents()).containsExactly(
                new IncomeAdded(planId, incomeId),
                new ExpenseAdded(planId, expenseId));
    }

    @Test
    void invalidItemIsNeitherAddedNorRecorded() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.addExpense("Miete", amount, null))
                .isInstanceOf(InvalidBudgetItemException.class);

        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void removesItemAndRecordsIt() {
        HouseholdPlan plan = emptyPlan();
        BudgetItemId itemId = plan.addIncome("Gehalt", amount);
        plan.pullDomainEvents();

        plan.removeItem(itemId);

        assertThat(plan.items()).isEmpty();
        assertThat(plan.pullDomainEvents()).containsExactly(new BudgetItemRemoved(planId, itemId));
    }

    @Test
    void removingAnItemOfAnotherPlanFails() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.removeItem(BudgetItemId.generate()))
                .isInstanceOf(BudgetItemNotFoundException.class);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void pullingEventsForgetsThem() {
        HouseholdPlan plan = HouseholdPlan.create(planId, YEAR);

        assertThat(plan.pullDomainEvents()).hasSize(1);
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    @Test
    void itemsCannotBeChangedFromOutside() {
        HouseholdPlan plan = emptyPlan();

        assertThatThrownBy(() -> plan.items().add(BudgetItem.income(BudgetItemId.generate(), "Gehalt", amount)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        HouseholdPlan first = HouseholdPlan.reconstitute(planId, YEAR, List.of());
        HouseholdPlan second = HouseholdPlan.reconstitute(planId, Year.of(2027), List.of());

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    private HouseholdPlan emptyPlan() {
        return HouseholdPlan.reconstitute(planId, YEAR, List.of());
    }
}
