package de.jcss.hbt.domain.onetimeitem.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Place of a one-time item within its area, the incomes or the expenses of a year (ES-4): items
 * with a lower rank come first. Ranks only order items, their size has no meaning; a rank between
 * two others can always be found, so moving an item never changes the rank of another one.
 * <p>
 * An item placed at the end of its area gets the time of placing in microseconds as rank (ES-7, ES-8),
 * which is after every item placed or moved before.
 */
@ValueObject
public record OneTimeItemRank(BigDecimal value) implements Comparable<OneTimeItemRank> {

    public OneTimeItemRank {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OneTimeItemRank of(BigDecimal value) {
        return new OneTimeItemRank(value);
    }

    /** The rank of an item placed at the end of its area at the given time (ES-7, ES-8). */
    public static OneTimeItemRank last(Instant placedAt) {
        return new OneTimeItemRank(BigDecimal.valueOf(placedAt.getEpochSecond()).scaleByPowerOfTen(6)
                .add(BigDecimal.valueOf(placedAt.getNano() / 1000)));
    }

    /**
     * A rank between two neighbors (ES-4).
     *
     * @param previous rank of the item that is to come right before, {@code null} at the start of the area
     * @param next     rank of the item that is to come right after, {@code null} at the end of the area
     * @throws DomainValidationException if both neighbors are missing
     */
    public static OneTimeItemRank between(OneTimeItemRank previous, OneTimeItemRank next) {
        if (previous == null && next == null) {
            throw new DomainValidationException("a moved one-time item needs at least one neighbor (ES-4)");
        }
        if (previous == null) {
            return new OneTimeItemRank(next.value.subtract(BigDecimal.ONE));
        }
        if (next == null) {
            return new OneTimeItemRank(previous.value.add(BigDecimal.ONE));
        }
        BigDecimal sum = previous.value.add(next.value);
        return new OneTimeItemRank(sum.divide(BigDecimal.TWO, sum.scale() + 1, RoundingMode.UNNECESSARY));
    }

    @Override
    public int compareTo(OneTimeItemRank other) {
        return value.compareTo(other.value);
    }
}
