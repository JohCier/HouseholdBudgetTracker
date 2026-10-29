package de.jcss.hbt.domain.networth.overview;

import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.Percentage;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sums and changes of the net worth (VS-6, VS-12, VS-13), with the example of the spec. */
class NetWorthHistoryTest {

    /** VS-12, VS-13, example table of the spec */
    @Test
    void comparesEveryYearWithTheYearBefore() {
        NetWorthHistory history = NetWorthHistory.of(List.of(
                totals(2024, "15200.00", "11400.00"),
                totals(2026, "21700.00", "7800.00"),
                totals(2023, "9000.00", "3000.00"),
                totals(2025, "20100.00", "9600.00")));

        assertThat(history.years()).extracting(entry -> entry.totals().year().getValue())
                .containsExactly(2026, 2025, 2024, 2023);
        assertThat(history.years().get(0).totals().netWorth()).isEqualTo(money("13900.00"));
        assertThat(history.years().get(0).change()).isEqualTo(change("3400.00", "32.38"));
        assertThat(history.years().get(1).change()).isEqualTo(change("6700.00", "176.32"));
        assertThat(history.years().get(2).change()).isEqualTo(change("-2200.00", "-36.67"));
        assertThat(history.years().get(3).change()).isNull();
    }

    /** VS-12: 2025 is not compared with 2023 if 2024 is missing */
    @Test
    void aMissingYearInterruptsTheComparison() {
        NetWorthHistory history = NetWorthHistory.of(List.of(
                totals(2023, "9000.00", "3000.00"),
                totals(2025, "20100.00", "9600.00")));

        assertThat(history.years()).extracting(NetWorthHistoryEntry::change).containsOnlyNulls();
    }

    /** VS-6, VS-13: a negative previous net worth still gives a positive change for growth */
    @Test
    void negativeNetWorthIsOverindebtedAndGrowthFromItIsPositive() {
        NetWorthHistory history = NetWorthHistory.of(List.of(
                totals(2025, "1000.00", "2000.00"),
                totals(2026, "1500.00", "2000.00")));

        assertThat(history.years().get(1).totals().overindebted()).isTrue();
        assertThat(history.years().get(0).change()).isEqualTo(change("500.00", "50.00"));
    }

    /** VS-13: no percentage after a net worth of zero, but the change in euros */
    @Test
    void noPercentageAfterZero() {
        NetWorthHistory history = NetWorthHistory.of(List.of(
                totals(2025, "500.00", "500.00"),
                totals(2026, "1500.00", "500.00")));

        assertThat(history.years().getFirst().change()).isEqualTo(new NetWorthChange(money("1000.00"), null));
    }

    /** VS-6, VS-8 */
    @Test
    void overviewSplitsAssetsAndLiabilitiesAndSumsThem() {
        NetWorthOverview overview = NetWorthOverview.of(Year.of(2026), List.of(
                entry(NetWorthEntryType.ASSET, "Girokonto", "3200.00"),
                entry(NetWorthEntryType.LIABILITY, "Autokredit", "6300.00"),
                entry(NetWorthEntryType.ASSET, "Tagesgeld", "6000.00"),
                entry(NetWorthEntryType.ASSET, "ETF-Depot", "12500.00"),
                entry(NetWorthEntryType.LIABILITY, "Ratenkauf Küche", "1500.00")),
                totals(2025, "20100.00", "9600.00"));

        assertThat(overview.assets()).extracting(entry -> entry.name().value())
                .containsExactly("Girokonto", "Tagesgeld", "ETF-Depot");
        assertThat(overview.liabilities()).extracting(entry -> entry.name().value())
                .containsExactly("Autokredit", "Ratenkauf Küche");
        assertThat(overview.totals().totalAssets()).isEqualTo(money("21700.00"));
        assertThat(overview.totals().totalLiabilities()).isEqualTo(money("7800.00"));
        assertThat(overview.totals().netWorth()).isEqualTo(money("13900.00"));
        assertThat(overview.change()).isEqualTo(change("3400.00", "32.38"));
        assertThat(overview.recorded()).isTrue();
    }

    /** VS-10 */
    @Test
    void aYearWithoutEntriesHasNoChange() {
        NetWorthOverview overview = NetWorthOverview.of(Year.of(2027), List.of(), totals(2026, "100.00", "0.00"));

        assertThat(overview.recorded()).isFalse();
        assertThat(overview.totals().netWorth()).isEqualTo(Money.ZERO);
        assertThat(overview.change()).isNull();
    }

    private static NetWorthTotals totals(int year, String assets, String liabilities) {
        return new NetWorthTotals(Year.of(year), money(assets), money(liabilities));
    }

    private static NetWorthOverviewEntry entry(NetWorthEntryType type, String name, String amount) {
        return new NetWorthOverviewEntry(NetWorthEntryId.generate(), type, NetWorthEntryName.of(name), money(amount));
    }

    private static NetWorthChange change(String amount, String percentage) {
        return new NetWorthChange(money(amount), new Percentage(new BigDecimal(percentage)));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
