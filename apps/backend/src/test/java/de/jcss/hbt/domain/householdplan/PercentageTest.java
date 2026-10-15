package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidPercentageException;
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
                .isInstanceOf(InvalidPercentageException.class)
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
    void comparesByValue() {
        assertThat(Percentage.of(20)).isLessThan(Percentage.of(30));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
