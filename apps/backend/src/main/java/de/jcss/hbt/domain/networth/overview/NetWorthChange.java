package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.Percentage;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.Objects;

/**
 * How the net worth of a year differs from that of the year before (VS-12, VS-13): in euros and in
 * percent of the amount of the previous net worth, like {@link Percentage#changeFrom}.
 *
 * @param percentage {@code null} if the previous net worth was zero
 */
@QueryModel
public record NetWorthChange(Money amount, Percentage percentage) {

    public NetWorthChange {
        Objects.requireNonNull(amount, "amount must not be null");
    }

    public static NetWorthChange between(Money previous, Money current) {
        return new NetWorthChange(current.subtract(previous), Percentage.changeFrom(previous, current).orElse(null));
    }
}
