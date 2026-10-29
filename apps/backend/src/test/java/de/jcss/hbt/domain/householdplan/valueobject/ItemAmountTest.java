package de.jcss.hbt.domain.householdplan.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Monthly and yearly amount of a budget item (MJ-6, MJ-7). */
class ItemAmountTest {

    /** MJ-6 */
    @Test
    void monthlyAmountTimesTwelveIsTheYearlyAmount() {
        ItemAmount rent = ItemAmount.monthly(money("850.00"));

        assertThat(rent.monthly()).isEqualTo(money("850.00"));
        assertThat(rent.annual()).isEqualTo(money("10200.00"));
    }

    /** MJ-7 */
    @Test
    void yearlyAmountDividedByTwelveIsRoundedHalfUpToTheCent() {
        ItemAmount insurance = ItemAmount.yearly(money("1000.00"));

        assertThat(insurance.annual()).isEqualTo(money("1000.00"));
        assertThat(insurance.monthly()).isEqualTo(money("83.33"));
        assertThat(ItemAmount.yearly(money("0.06")).monthly()).isEqualTo(money("0.01"));
        assertThat(ItemAmount.yearly(money("0.05")).monthly()).isEqualTo(money("0.00"));
    }

    /** MJ-2 */
    @Test
    void keepsTheEnteredAmountAndItsPeriod() {
        ItemAmount amount = ItemAmount.monthly(money("12.34"));

        assertThat(amount.value()).isEqualTo(money("12.34"));
        assertThat(amount.period()).isEqualTo(AmountPeriod.MONTHLY);
        assertThat(amount).isNotEqualTo(ItemAmount.yearly(money("12.34")));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
