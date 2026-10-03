package com.example.householdbudgettracker.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a monetary amount with a fixed scale of 2 decimal places.
 * Deliberately allows zero and negative values, since it is also used for derived
 * figures such as balances; positivity of a single budget item is a rule enforced
 * by {@link BudgetItem}, not by this value object.
 */
public record Money(BigDecimal value) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(value, "value must not be null");
        value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public Money add(Money other) {
        return new Money(this.value.add(other.value));
    }

    public Money subtract(Money other) {
        return new Money(this.value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        return this.value.compareTo(other.value);
    }
}
