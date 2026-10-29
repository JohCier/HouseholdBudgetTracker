package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing a share in percent with a precision of two decimal places,
 * e.g. {@code 34.50} for 34.5 %. Values with more decimal places are rejected; shares
 * computed via {@link #shareOf} and {@link #changeFrom} are rounded half up instead. May be
 * negative, e.g. for a decrease.
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

    /**
     * By how many percent {@code current} differs from {@code previous}, relative to the amount of
     * {@code previous} (JU-5): positive if {@code current} is higher, even if {@code previous} is
     * negative. Empty if {@code previous} is zero, as no change in percent follows from it (JU-6).
     */
    public static Optional<Percentage> changeFrom(Money previous, Money current) {
        if (previous.isZero()) {
            return Optional.empty();
        }
        BigDecimal difference = current.value().subtract(previous.value());
        return Optional.of(new Percentage(
                difference.multiply(HUNDRED).divide(previous.value().abs(), 2, RoundingMode.HALF_UP)));
    }

    @Override
    public int compareTo(Percentage other) {
        return this.value.compareTo(other.value);
    }
}
