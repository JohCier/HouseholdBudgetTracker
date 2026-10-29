package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.event.BudgetItemMarked;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** Marking items as important and with a saving potential (PM-1 to PM-8, PM-13). */
class HouseholdPlanMarksTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final BudgetItemDescription STREAMING = BudgetItemDescription.of("Streaming");

    private final HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
    private BudgetItemId salaryId;
    private BudgetItemId streamingId;

    @BeforeEach
    void fillPlan() {
        salaryId = plan.addIncome(BudgetItemDescription.of("Gehalt"), monthly("3000.00"), null, null);
        streamingId = plan.addExpense(STREAMING, monthly("15.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);
        plan.pullDomainEvents();
    }

    /** PM-1, PM-2 */
    @Test
    void newItemsHaveNoMarks() {
        assertThat(plan.items()).extracting(BudgetItem::isImportant, BudgetItem::savingPotential)
                .containsOnly(tuple(false, Optional.empty()));
    }

    /** PM-1, PM-4a, PM-7 */
    @Test
    void marksAnItemAsImportantAndBackAndRecordsIt() {
        plan.markImportant(salaryId, true);

        assertThat(item(salaryId).isImportant()).isTrue();
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemMarked(plan.id(), salaryId, BudgetItemMarked.Mark.IMPORTANT, true, Instant.now()));

        plan.markImportant(salaryId, false);

        assertThat(item(salaryId).isImportant()).isFalse();
        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new BudgetItemMarked(plan.id(), salaryId, BudgetItemMarked.Mark.IMPORTANT, false, Instant.now()));
    }

    @Test
    void markingAsItIsRecordsNothing() {
        plan.markImportant(salaryId, false);

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PM-7: the order stays */
    @Test
    void markingKeepsThePositionOfTheItem() {
        plan.markImportant(salaryId, true);
        plan.changeSavingPotential(streamingId, potential("5.00"));

        assertThat(plan.items()).extracting(BudgetItem::id).containsExactly(salaryId, streamingId);
    }

    /** PM-2, PM-2a, PM-4b, PM-7 */
    @Test
    void setsChangesAndRemovesTheSavingPotentialOfAnExpense() {
        plan.changeSavingPotential(streamingId, potential("5.00"));
        assertThat(item(streamingId).savingPotential()).contains(potential("5.00"));

        plan.changeSavingPotential(streamingId, potential("15.00"));
        assertThat(item(streamingId).savingPotential()).contains(potential("15.00"));

        plan.changeSavingPotential(streamingId, null);
        assertThat(item(streamingId).savingPotential()).isEmpty();

        assertThat(plan.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(
                        new BudgetItemMarked(plan.id(), streamingId, BudgetItemMarked.Mark.SAVING_POTENTIAL, true, Instant.now()),
                        new BudgetItemMarked(plan.id(), streamingId, BudgetItemMarked.Mark.SAVING_POTENTIAL, true, Instant.now()),
                        new BudgetItemMarked(plan.id(), streamingId, BudgetItemMarked.Mark.SAVING_POTENTIAL, false, Instant.now()));
    }

    @Test
    void keepingTheSameSavingPotentialRecordsNothing() {
        plan.changeSavingPotential(streamingId, potential("5.00"));
        plan.pullDomainEvents();

        plan.changeSavingPotential(streamingId, potential("5.00"));

        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PM-2 */
    @Test
    void anIncomeCannotHaveASavingPotential() {
        assertThatThrownBy(() -> plan.changeSavingPotential(salaryId, potential("5.00")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("income items must not have a saving potential");
        assertThat(item(salaryId).savingPotential()).isEmpty();
        assertThat(plan.pullDomainEvents()).isEmpty();
    }

    /** PM-2a */
    @Test
    void theSavingPotentialMustNotExceedTheMonthlyAmount() {
        assertThatThrownBy(() -> plan.changeSavingPotential(streamingId, potential("15.01")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("saving potential must not exceed the monthly amount of the expense, 15.00");
        assertThat(item(streamingId).savingPotential()).isEmpty();
    }

    /** PM-2a: an expense of 360.00 a year has 30.00 a month */
    @Test
    void theSavingPotentialOfAYearlyExpenseIsComparedWithItsMonthlyAmount() {
        BudgetItemId gymId = plan.addExpense(BudgetItemDescription.of("Fitnessstudio"), yearly("360.00"),
                ExpenseCategory.FREIZEIT_SPASS, null, null);

        plan.changeSavingPotential(gymId, potential("30.00"));

        assertThat(item(gymId).savingPotential()).contains(potential("30.00"));
        assertThatThrownBy(() -> plan.changeSavingPotential(gymId, potential("30.01")))
                .isInstanceOf(DomainValidationException.class);
    }

    /** PM-3 */
    @Test
    void bothMarksAreIndependent() {
        plan.markImportant(streamingId, true);
        plan.changeSavingPotential(streamingId, potential("5.00"));
        plan.markImportant(streamingId, false);

        assertThat(item(streamingId).isImportant()).isFalse();
        assertThat(item(streamingId).savingPotential()).contains(potential("5.00"));
    }

    /** PM-5 */
    @Test
    void changingAnItemKeepsItsMarks() {
        plan.markImportant(streamingId, true);
        plan.changeSavingPotential(streamingId, potential("5.00"));

        plan.changeItem(streamingId, STREAMING, monthly("20.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);

        assertThat(item(streamingId).isImportant()).isTrue();
        assertThat(item(streamingId).savingPotential()).contains(potential("5.00"));
    }

    /** PM-5 */
    @Test
    void changingTheAmountBelowTheSavingPotentialLowersIt() {
        plan.changeSavingPotential(streamingId, potential("5.00"));

        plan.changeItem(streamingId, STREAMING, yearly("36.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);

        assertThat(item(streamingId).savingPotential()).contains(potential("3.00"));
    }

    /** PM-6 */
    @Test
    void addsARestoredItemWithItsMarks() {
        BudgetItemId restoredId = plan.addExpense(STREAMING, monthly("15.00"), ExpenseCategory.FREIZEIT_SPASS,
                null, null, true, potential("5.00"));

        assertThat(item(restoredId).isImportant()).isTrue();
        assertThat(item(restoredId).savingPotential()).contains(potential("5.00"));
    }

    /** PM-2a; an income cannot even be given a saving potential when it is restored (PM-2) */
    @Test
    void rejectsARestoredExpenseWithASavingPotentialAboveItsMonthlyAmount() {
        assertThatThrownBy(() -> plan.addExpense(STREAMING, monthly("15.00"), ExpenseCategory.FREIZEIT_SPASS,
                null, null, false, potential("20.00")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(plan.items()).hasSize(2);
    }

    /** PM-6 */
    @Test
    void aCopyOfThePlanKeepsTheMarks() {
        plan.markImportant(salaryId, true);
        plan.changeSavingPotential(streamingId, potential("5.00"));

        HouseholdPlan copy = HouseholdPlan.createFrom(plan, HouseholdPlanId.generate(), Year.of(2027), false);

        assertThat(copy.items()).extracting(BudgetItem::isImportant, BudgetItem::savingPotential)
                .containsExactly(tuple(true, Optional.empty()), tuple(false, Optional.of(potential("5.00"))));
    }

    /** PM-8 */
    @Test
    void marksDoNotAffectTheSummary() {
        BudgetSummary before = plan.summary();

        plan.markImportant(streamingId, true);
        plan.changeSavingPotential(streamingId, potential("5.00"));

        assertThat(plan.summary()).isEqualTo(before);
    }

    @Test
    void markingAnItemOfAnotherPlanFails() {
        BudgetItemId unknown = BudgetItemId.generate();

        assertThatThrownBy(() -> plan.markImportant(unknown, true)).isInstanceOf(BudgetItemNotFoundException.class);
        assertThatThrownBy(() -> plan.changeSavingPotential(unknown, null))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    private BudgetItem item(BudgetItemId id) {
        return plan.items().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private static ItemAmount monthly(String value) {
        return ItemAmount.monthly(Money.of(new BigDecimal(value)));
    }

    private static ItemAmount yearly(String value) {
        return ItemAmount.yearly(Money.of(new BigDecimal(value)));
    }

    private static SavingPotential potential(String value) {
        return SavingPotential.monthly(Money.of(new BigDecimal(value)));
    }
}
