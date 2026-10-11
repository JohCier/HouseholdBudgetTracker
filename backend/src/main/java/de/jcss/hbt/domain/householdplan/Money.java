package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidMoneyException;
import de.jcss.hbt.domain.shared.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a monetary amount in cents precision (scale 2).
 * Values with more than two significant decimal places are rejected instead of being
 * rounded silently. Deliberately allows zero and negative values, since it is also used
 * for derived figures such as balances; positivity of a single budget item is a rule
 * enforced by {@link BudgetItem}, not by this value object.
 */
@ValueObject
public record Money(BigDecimal value) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(value, "value must not be null");
        if (value.stripTrailingZeros().scale() > 2) {
            throw new InvalidMoneyException("amount must not have more than 2 decimal places");
        }
        value = value.setScale(2);
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
