package de.jcss.hbt.domain.householdplan;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseCategoryTest {

    private static final Money INCOME = money("1000.00");

    @Test
    void ceilingIsMetUpToTheTargetShare() {
        assertThat(ExpenseCategory.LEBENSUNTERHALT.isTargetMet(money("500.00"), INCOME)).isTrue();
        assertThat(ExpenseCategory.LEBENSUNTERHALT.isTargetMet(money("501.00"), INCOME)).isFalse();
    }

    @Test
    void floorIsMetFromTheTargetShare() {
        assertThat(ExpenseCategory.SPAREN_INVESTIEREN.isTargetMet(money("199.00"), INCOME)).isFalse();
        assertThat(ExpenseCategory.SPAREN_INVESTIEREN.isTargetMet(money("200.00"), INCOME)).isTrue();
    }

    @Test
    void withoutIncomeSpendingBreaksCeilingsButNotFloors() {
        assertThat(ExpenseCategory.FREIZEIT_SPASS.isTargetMet(money("50.00"), Money.ZERO)).isFalse();
        assertThat(ExpenseCategory.FREIZEIT_SPASS.isTargetMet(Money.ZERO, Money.ZERO)).isTrue();
        assertThat(ExpenseCategory.SPAREN_INVESTIEREN.isTargetMet(Money.ZERO, Money.ZERO)).isTrue();
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
