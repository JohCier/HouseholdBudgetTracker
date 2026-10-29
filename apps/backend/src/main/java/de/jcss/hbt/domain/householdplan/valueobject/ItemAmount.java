package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.householdplan.BudgetItem;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * The amount of a {@link BudgetItem} as the user entered it, together with whether it is a
 * monthly or a yearly amount (MJ-1, MJ-2). The yearly amount is what all figures are worked out
 * from (MJ-8); the monthly amount is only shown (MJ-6, MJ-7).
 */
@ValueObject
public record ItemAmount(Money value, AmountPeriod period) {

    public ItemAmount {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(period, "period must not be null");
    }

    public static ItemAmount monthly(Money value) {
        return new ItemAmount(value, AmountPeriod.MONTHLY);
    }

    public static ItemAmount yearly(Money value) {
        return new ItemAmount(value, AmountPeriod.YEARLY);
    }

    /** The amount for the whole year: a monthly amount times 12 (MJ-6). */
    public Money annual() {
        return switch (period) {
            case MONTHLY -> value.timesMonthsPerYear();
            case YEARLY -> value;
        };
    }

    /** The amount per month: a yearly amount divided by 12, rounded half up to the cent (MJ-7). */
    public Money monthly() {
        return switch (period) {
            case MONTHLY -> value;
            case YEARLY -> value.perMonth();
        };
    }

    public boolean isPositive() {
        return value.isPositive();
    }

    @Override
    public String toString() {
        return value.value().toPlainString() + " " + period;
    }
}
