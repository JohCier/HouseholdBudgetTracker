package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * The part of an expense the user thinks could be saved, always per month, whatever the period of
 * the expense: what counts is how much more would be left each month (PM-2, PM-2a). It is positive;
 * that it does not exceed the monthly amount of its expense is enforced by {@link BudgetItem}.
 */
@ValueObject
public record SavingPotential(Money monthly) {

    public SavingPotential {
        Objects.requireNonNull(monthly, "monthly must not be null");
        if (!monthly.isPositive()) {
            throw new DomainValidationException("saving potential must be positive");
        }
    }

    public static SavingPotential monthly(Money monthly) {
        return new SavingPotential(monthly);
    }

    /** The saving potential for the whole year: twelve times the monthly one (PM-13). */
    public Money annual() {
        return monthly.timesMonthsPerYear();
    }

    /** Whether it is more than the monthly amount of the given expense amount (PM-2a). */
    public boolean exceeds(ItemAmount amount) {
        return monthly.compareTo(amount.monthly()) > 0;
    }

    /** This saving potential, lowered to the monthly amount of the given expense amount if it exceeds it (PM-5). */
    public SavingPotential cappedTo(ItemAmount amount) {
        return exceeds(amount) ? new SavingPotential(amount.monthly()) : this;
    }
}
