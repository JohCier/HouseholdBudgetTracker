package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.networth.NetWorthEntryType;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * Read model of the page "Vermögen" of a year (VS-8): its assets and its liabilities, each in the order
 * they were recorded, their sums and the change of the net worth from the year before (VS-12). A year
 * without statement has no entries and zero sums (VS-10).
 *
 * @param change {@code null} if this or the previous year has no statement
 */
@QueryModel
public record NetWorthOverview(NetWorthTotals totals, List<NetWorthOverviewEntry> assets,
                               List<NetWorthOverviewEntry> liabilities, NetWorthChange change) {

    public NetWorthOverview {
        Objects.requireNonNull(totals, "totals must not be null");
        assets = List.copyOf(assets);
        liabilities = List.copyOf(liabilities);
    }

    /**
     * @param entries  all entries of the year in the order they were recorded
     * @param previous the sums of the year before; {@code null} if it has no statement
     */
    public static NetWorthOverview of(Year year, List<NetWorthOverviewEntry> entries, NetWorthTotals previous) {
        List<NetWorthOverviewEntry> assets = entriesOf(entries, NetWorthEntryType.ASSET);
        List<NetWorthOverviewEntry> liabilities = entriesOf(entries, NetWorthEntryType.LIABILITY);
        NetWorthTotals totals = new NetWorthTotals(year, sumOf(assets), sumOf(liabilities));
        return new NetWorthOverview(totals, assets, liabilities, entries.isEmpty() ? null : totals.changeFrom(previous));
    }

    private static List<NetWorthOverviewEntry> entriesOf(List<NetWorthOverviewEntry> entries, NetWorthEntryType type) {
        return entries.stream().filter(entry -> entry.type() == type).toList();
    }

    private static Money sumOf(List<NetWorthOverviewEntry> entries) {
        return entries.stream().map(NetWorthOverviewEntry::amount).reduce(Money.ZERO, Money::add);
    }

    public Year year() {
        return totals.year();
    }

    /** Whether the year has a statement, i.e. at least one entry (VS-7). */
    public boolean recorded() {
        return !assets.isEmpty() || !liabilities.isEmpty();
    }
}
