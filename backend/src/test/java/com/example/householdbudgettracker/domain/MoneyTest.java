package com.example.householdbudgettracker.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void scalesValueToTwoDecimalPlaces() {
        Money money = Money.of(new BigDecimal("10.5"));

        assertThat(money.value()).isEqualByComparingTo("10.50");
    }

    @Test
    void addsTwoAmounts() {
        Money result = Money.of(new BigDecimal("10.00")).add(Money.of(new BigDecimal("5.50")));

        assertThat(result.value()).isEqualByComparingTo("15.50");
    }

    @Test
    void subtractsTwoAmounts() {
        Money result = Money.of(new BigDecimal("10.00")).subtract(Money.of(new BigDecimal("4.00")));

        assertThat(result.value()).isEqualByComparingTo("6.00");
    }

    @Test
    void subtractionCanGoNegative() {
        Money result = Money.of(new BigDecimal("5.00")).subtract(Money.of(new BigDecimal("10.00")));

        assertThat(result.value()).isEqualByComparingTo("-5.00");
        assertThat(result.isPositive()).isFalse();
    }

    @Test
    void identifiesZero() {
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.of(BigDecimal.ZERO).isZero()).isTrue();
        assertThat(Money.of(new BigDecimal("0.01")).isZero()).isFalse();
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> Money.of(null)).isInstanceOf(NullPointerException.class);
    }
}
