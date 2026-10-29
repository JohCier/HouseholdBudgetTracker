package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.valueobject.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.Objects;

/**
 * The sums of a year (VS-6): its assets, its liabilities and the difference, the net worth, which may
 * be negative.
 */
@QueryModel
public record NetWorthTotals(Year year, Money totalAssets, Money totalLiabilities) {

    public NetWorthTotals {
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(totalAssets, "totalAssets must not be null");
        Objects.requireNonNull(totalLiabilities, "totalLiabilities must not be null");
    }

    public Money netWorth() {
        return totalAssets.subtract(totalLiabilities);
    }

    /** Whether the liabilities exceed the assets, shown as "Überschuldet" (VS-6). */
    public boolean overindebted() {
        return netWorth().value().signum() < 0;
    }

    /** The change from the given year, if it is the year immediately before this one; otherwise none (VS-12). */
    NetWorthChange changeFrom(NetWorthTotals previous) {
        if (previous == null || !previous.year.equals(year.minusYears(1))) {
            return null;
        }
        return NetWorthChange.between(previous.netWorth(), netWorth());
    }
}
