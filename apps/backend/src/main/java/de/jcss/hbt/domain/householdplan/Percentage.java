package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a share in percent with a precision of two decimal places,
 * e.g. {@code 34.50} for 34.5 %. Values with more decimal places are rejected; shares
 * computed via {@link #shareOf} are rounded half up instead.
 */
@ValueObject
public record Percentage(BigDecimal value) implements Comparable<Percentage> {

    public static final Percentage ZERO = new Percentage(BigDecimal.ZERO);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Percentage {
        Objects.requireNonNull(value, "value must not be null");
        if (value.stripTrailingZeros().scale() > 2) {
            throw new DomainValidationException("percentage must not have more than 2 decimal places");
        }
        value = value.setScale(2);
    }

    public static Percentage of(int value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    /** How many percent {@code part} makes up of {@code whole}; zero if {@code whole} is zero. */
    public static Percentage shareOf(Money part, Money whole) {
        if (whole.isZero()) {
            return ZERO;
        }
        return new Percentage(part.value().multiply(HUNDRED).divide(whole.value(), 2, RoundingMode.HALF_UP));
    }

    @Override
    public int compareTo(Percentage other) {
        return this.value.compareTo(other.value);
    }
}
