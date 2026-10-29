package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PercentageTest {

    @Test
    void scalesValueToTwoDecimalPlaces() {
        assertThat(Percentage.of(50).value()).isEqualTo(new BigDecimal("50.00"));
        assertThat(Percentage.of(50)).isEqualTo(new Percentage(new BigDecimal("50.0")));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> new Percentage(new BigDecimal("33.333")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("decimal places");
    }

    @Test
    void shareIsRoundedHalfUp() {
        assertThat(Percentage.shareOf(money("1.00"), money("3.00")).value()).isEqualByComparingTo("33.33");
        assertThat(Percentage.shareOf(money("2.00"), money("3.00")).value()).isEqualByComparingTo("66.67");
        assertThat(Percentage.shareOf(money("350.00"), money("1000.00"))).isEqualTo(Percentage.of(35));
    }

    @Test
    void shareOfNothingIsZero() {
        assertThat(Percentage.shareOf(money("50.00"), Money.ZERO)).isEqualTo(Percentage.ZERO);
    }

    @Test
    void changeIsRelativeToThePreviousValueAndRoundedHalfUp() {
        assertThat(Percentage.changeFrom(money("30000.00"), money("30900.00"))).contains(Percentage.of(3));
        assertThat(Percentage.changeFrom(money("25000.00"), money("24250.00"))).contains(Percentage.of(-3));
        assertThat(Percentage.changeFrom(money("3.00"), money("4.00")).orElseThrow().value()).isEqualByComparingTo("33.33");
        assertThat(Percentage.changeFrom(money("3.00"), money("5.00")).orElseThrow().value()).isEqualByComparingTo("66.67");
        assertThat(Percentage.changeFrom(money("100.00"), money("100.00"))).contains(Percentage.ZERO);
    }

    @Test
    void changeFromANegativeValueIsPositiveIfTheValueGrew() {
        assertThat(Percentage.changeFrom(money("-1000.00"), money("-500.00"))).contains(Percentage.of(50));
        assertThat(Percentage.changeFrom(money("-1000.00"), money("-1500.00"))).contains(Percentage.of(-50));
        assertThat(Percentage.changeFrom(money("-1000.00"), money("1000.00"))).contains(Percentage.of(200));
    }

    @Test
    void noChangeFromZero() {
        assertThat(Percentage.changeFrom(Money.ZERO, money("1000.00"))).isEmpty();
        assertThat(Percentage.changeFrom(Money.ZERO, Money.ZERO)).isEmpty();
    }

    @Test
    void comparesByValue() {
        assertThat(Percentage.of(20)).isLessThan(Percentage.of(30));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
