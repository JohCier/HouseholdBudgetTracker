package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverviewQueryHandler;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeOverviewItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

/**
 * Reads the one-time items of a year straight from the {@code one_time_item} table, without
 * reconstituting the {@code OneTimeItem} aggregate; by date, on the same date in the order they
 * were entered (EP-8).
 */
@Component
public class JdbcOneTimeItemOverviewQueryHandler implements OneTimeItemOverviewQueryHandler {

    private final JdbcClient jdbcClient;

    public JdbcOneTimeItemOverviewQueryHandler(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public OneTimeItemOverview findByOwnerAndYear(OwnerId owner, Year year) {
        return new OneTimeItemOverview(year, jdbcClient.sql("""
                        SELECT id, type, description, amount, item_date, note
                        FROM one_time_item
                        WHERE owner_id = :owner AND item_date BETWEEN :first AND :last
                        ORDER BY item_date, created_at, id
                        """)
                .param("owner", owner.value())
                .param("first", year.atDay(1))
                .param("last", year.atMonth(12).atEndOfMonth())
                .query((rs, rowNum) -> new OneTimeOverviewItem(
                        OneTimeItemId.of(rs.getObject("id", UUID.class)),
                        ItemType.valueOf(rs.getString("type")),
                        BudgetItemDescription.of(rs.getString("description")),
                        Money.of(rs.getBigDecimal("amount")),
                        rs.getObject("item_date", LocalDate.class),
                        BudgetItemNote.ofNullable(rs.getString("note"))))
                .list());
    }
}
