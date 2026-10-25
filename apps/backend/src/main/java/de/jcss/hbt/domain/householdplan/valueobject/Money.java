package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    public Money {
        Objects.requireNonNull(value, "value must not be null");
        if (value.stripTrailingZeros().scale() > 2) {
            throw new DomainValidationException("amount must not have more than 2 decimal places");
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

    /** This amount taken as a monthly amount, for the whole year (MJ-6). */
    public Money timesMonthsPerYear() {
        return new Money(value.multiply(MONTHS_PER_YEAR));
    }

    /** This amount taken as a yearly amount, per month, rounded half up to the cent (MJ-7, MJ-9). */
    public Money perMonth() {
        return new Money(value.divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP));
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
