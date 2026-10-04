package de.jcss.hbt.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetOverviewTest {

    private static final Year YEAR = Year.of(2026);
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Test
    void withNoItemsEverythingIsZeroAndNoTargetIsMissed() {
        BudgetOverview overview = BudgetOverview.of(YEAR, List.of());

        assertThat(overview.year()).isEqualTo(YEAR);
        assertThat(overview.totalIncome().isZero()).isTrue();
        assertThat(overview.totalExpense().isZero()).isTrue();
        assertThat(overview.balance().isZero()).isTrue();
        assertThat(overview.expensesExceedIncome()).isFalse();
        assertThat(overview.categoryBreakdowns()).allSatisfy(breakdown -> {
            assertThat(breakdown.total().isZero()).isTrue();
            assertThat(breakdown.actualPercentageOfIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(breakdown.targetMet()).isTrue();
        });
    }

    @Test
    void calculatesTotalsAndBalance() {
        BudgetOverview overview = BudgetOverview.of(YEAR, List.of(
                income("2000.00"),
                expense("1000.00", ExpenseCategory.LEBENSUNTERHALT),
                expense("100.00", ExpenseCategory.FREIZEIT_SPASS)
        ));

        assertThat(overview.totalIncome().value()).isEqualByComparingTo("2000.00");
        assertThat(overview.totalExpense().value()).isEqualByComparingTo("1100.00");
        assertThat(overview.balance().value()).isEqualByComparingTo("900.00");
        assertThat(overview.expensesExceedIncome()).isFalse();
    }

    @Test
    void flagsWhenExpensesExceedIncome() {
        BudgetOverview overview = BudgetOverview.of(YEAR, List.of(
                income("500.00"),
                expense("800.00", ExpenseCategory.LEBENSUNTERHALT)
        ));

        assertThat(overview.expensesExceedIncome()).isTrue();
        assertThat(overview.balance().value()).isEqualByComparingTo("-300.00");
    }

    @Test
    void ceilingsAreMetUpToTheTargetShare() {
        BudgetOverview overview = BudgetOverview.of(YEAR, List.of(
                income("1000.00"),
                // exactly at the 50% ceiling
                expense("500.00", ExpenseCategory.LEBENSUNTERHALT),
                // above the 30% ceiling
                expense("350.00", ExpenseCategory.FREIZEIT_SPASS)
        ));

        CategoryBreakdown lebensunterhalt = breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT);
        assertThat(lebensunterhalt.total().value()).isEqualByComparingTo("500.00");
        assertThat(lebensunterhalt.actualPercentageOfIncome()).isEqualByComparingTo("50.00");
        assertThat(lebensunterhalt.targetPercentageOfIncome()).isEqualTo(50);
        assertThat(lebensunterhalt.targetDirection()).isEqualTo(TargetDirection.AT_MOST);
        assertThat(lebensunterhalt.targetMet()).isTrue();

        CategoryBreakdown freizeit = breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS);
        assertThat(freizeit.actualPercentageOfIncome()).isEqualByComparingTo("35.00");
        assertThat(freizeit.targetMet()).isFalse();
    }

    @Test
    void savingsAreAFloorThatMayBeExceeded() {
        BudgetOverview belowFloor = BudgetOverview.of(YEAR, List.of(
                income("1000.00"), expense("100.00", ExpenseCategory.SPAREN_INVESTIEREN)));
        BudgetOverview aboveFloor = BudgetOverview.of(YEAR, List.of(
                income("1000.00"), expense("300.00", ExpenseCategory.SPAREN_INVESTIEREN)));

        CategoryBreakdown tooLittle = breakdownFor(belowFloor, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(tooLittle.targetDirection()).isEqualTo(TargetDirection.AT_LEAST);
        assertThat(tooLittle.actualPercentageOfIncome()).isEqualByComparingTo("10.00");
        assertThat(tooLittle.targetMet()).isFalse();

        CategoryBreakdown plenty = breakdownFor(aboveFloor, ExpenseCategory.SPAREN_INVESTIEREN);
        assertThat(plenty.actualPercentageOfIncome()).isEqualByComparingTo("30.00");
        assertThat(plenty.targetMet()).isTrue();
    }

    @Test
    void spendingWithoutIncomeMissesCeilings() {
        BudgetOverview overview = BudgetOverview.of(YEAR, List.of(expense("50.00", ExpenseCategory.FREIZEIT_SPASS)));

        assertThat(breakdownFor(overview, ExpenseCategory.FREIZEIT_SPASS).targetMet()).isFalse();
        assertThat(breakdownFor(overview, ExpenseCategory.LEBENSUNTERHALT).targetMet()).isTrue();
        assertThat(breakdownFor(overview, ExpenseCategory.SPAREN_INVESTIEREN).targetMet()).isTrue();
    }

    @Test
    void rejectsItemsOfAnotherYear() {
        BudgetItem nextYear = BudgetItem.income(BudgetItemId.generate(), "Gehalt",
                Money.of(new BigDecimal("1.00")), LocalDate.of(2027, 1, 1));

        assertThatThrownBy(() -> BudgetOverview.of(YEAR, List.of(nextYear)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BudgetItem income(String amount) {
        return BudgetItem.income(BudgetItemId.generate(), "Gehalt", Money.of(new BigDecimal(amount)), DATE);
    }

    private static BudgetItem expense(String amount, ExpenseCategory category) {
        return BudgetItem.expense(BudgetItemId.generate(), "Ausgabe", Money.of(new BigDecimal(amount)), category, DATE);
    }

    private static CategoryBreakdown breakdownFor(BudgetOverview overview, ExpenseCategory category) {
        return overview.categoryBreakdowns().stream()
                .filter(breakdown -> breakdown.category() == category)
                .findFirst()
                .orElseThrow();
    }
}
