package de.jcss.hbt.domain.networth.overview;

import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read model of the card "Vermögen" of the yearly overview and its chart (VS-18, VS-20): all years
 * of a user with a statement, the newest first, each with its sums and the change of its net worth
 * from the year immediately before, if that one has a statement (VS-12).
 */
@QueryModel
public record NetWorthHistory(List<NetWorthHistoryEntry> years) {

    public NetWorthHistory {
        years = List.copyOf(years);
    }

    /** @param totals the sums of every year with a statement, in any order */
    public static NetWorthHistory of(List<NetWorthTotals> totals) {
        List<NetWorthTotals> ascending = totals.stream().sorted(Comparator.comparing(NetWorthTotals::year)).toList();
        List<NetWorthHistoryEntry> years = new ArrayList<>();
        NetWorthTotals previous = null;
        for (NetWorthTotals current : ascending) {
            years.addFirst(new NetWorthHistoryEntry(current, current.changeFrom(previous)));
            previous = current;
        }
        return new NetWorthHistory(years);
    }
}
