package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.overview.NetWorthHistory;
import de.jcss.hbt.domain.networth.overview.NetWorthOverview;
import de.jcss.hbt.domain.networth.overview.NetWorthOverviewEntry;
import de.jcss.hbt.domain.networth.overview.NetWorthQueryHandler;
import de.jcss.hbt.domain.networth.overview.NetWorthTotals;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Reads the net worth read models straight from the tables, without reconstituting the
 * {@code NetWorthStatement} aggregate. Entries are listed in the order they were recorded, see
 * {@link JdbcNetWorthStatementRepository}.
 */
@Component
public class JdbcNetWorthQueryHandler implements NetWorthQueryHandler {

    private final JdbcClient jdbcClient;

    public JdbcNetWorthQueryHandler(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public NetWorthOverview findByOwnerAndYear(OwnerId owner, Year year) {
        List<NetWorthOverviewEntry> entries = jdbcClient.sql("""
                        SELECT e.id, e.type, e.name, e.amount
                        FROM net_worth_entry e
                        JOIN net_worth_statement s ON s.id = e.net_worth_statement_id
                        WHERE s.owner_id = :owner AND s.year = :year
                        ORDER BY e.position
                        """)
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query((rs, rowNum) -> new NetWorthOverviewEntry(
                        NetWorthEntryId.of(rs.getObject("id", UUID.class)),
                        NetWorthEntryType.valueOf(rs.getString("type")),
                        NetWorthEntryName.of(rs.getString("name")),
                        Money.of(rs.getBigDecimal("amount"))))
                .list();
        List<NetWorthTotals> previous = totalsOf(owner, year.minusYears(1), year.minusYears(1));
        return NetWorthOverview.of(year, entries, previous.isEmpty() ? null : previous.getFirst());
    }

    @Override
    public NetWorthHistory findHistoryByOwner(OwnerId owner, Year firstYear, Year lastYear) {
        return NetWorthHistory.of(totalsOf(owner, firstYear, lastYear));
    }

    /** The sums of every year from {@code firstYear} to {@code lastYear} the owner has a statement for. */
    private List<NetWorthTotals> totalsOf(OwnerId owner, Year firstYear, Year lastYear) {
        return jdbcClient.sql("""
                        SELECT s.year,
                               COALESCE(SUM(e.amount) FILTER (WHERE e.type = 'ASSET'), 0)     AS assets,
                               COALESCE(SUM(e.amount) FILTER (WHERE e.type = 'LIABILITY'), 0) AS liabilities
                        FROM net_worth_statement s
                        JOIN net_worth_entry e ON e.net_worth_statement_id = s.id
                        WHERE s.owner_id = :owner AND s.year BETWEEN :firstYear AND :lastYear
                        GROUP BY s.year
                        """)
                .param("owner", owner.value())
                .param("firstYear", firstYear.getValue())
                .param("lastYear", lastYear.getValue())
                .query((rs, rowNum) -> new NetWorthTotals(Year.of(rs.getInt("year")),
                        Money.of(rs.getBigDecimal("assets")), Money.of(rs.getBigDecimal("liabilities"))))
                .list();
    }
}
