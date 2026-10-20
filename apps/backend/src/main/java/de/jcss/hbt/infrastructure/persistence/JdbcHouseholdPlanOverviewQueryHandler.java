package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.AmountPeriod;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the overview straight from the tables, without reconstituting the
 * {@code HouseholdPlan} aggregate. Items are listed in display order, the highest position on
 * top (UK-14), see {@link JdbcHouseholdPlanRepository}.
 */
@Component
public class JdbcHouseholdPlanOverviewQueryHandler implements HouseholdPlanOverviewQueryHandler {

    private static final RowMapper<OverviewItem> ITEM_ROW_MAPPER = (rs, rowNum) -> {
        String category = rs.getString("category");
        return new OverviewItem(
                BudgetItemId.of(rs.getObject("id", UUID.class)),
                BudgetItemDescription.of(rs.getString("description")),
                new ItemAmount(Money.of(rs.getBigDecimal("amount")), AmountPeriod.valueOf(rs.getString("amount_period"))),
                ItemType.valueOf(rs.getString("type")),
                category == null ? null : ExpenseCategory.valueOf(category),
                BudgetItemSubcategory.ofNullable(rs.getString("subcategory")),
                BudgetItemNote.ofNullable(rs.getString("note")));
    };

    private final JdbcClient jdbcClient;

    public JdbcHouseholdPlanOverviewQueryHandler(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<HouseholdPlanOverview> findByOwnerAndYear(OwnerId owner, Year year) {
        return jdbcClient.sql("SELECT id FROM household_plan WHERE owner_id = :owner AND year = :year")
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query((rs, rowNum) -> HouseholdPlanId.of(rs.getObject("id", UUID.class)))
                .optional()
                .map(planId -> HouseholdPlanOverview.of(planId, year, itemsOf(planId),
                        ItemSectionColumn.subcategoryOrdersOf(jdbcClient, planId)));
    }

    private List<OverviewItem> itemsOf(HouseholdPlanId planId) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, amount_period, type, category, subcategory, note
                        FROM budget_item
                        WHERE household_plan_id = :planId
                        ORDER BY position DESC
                        """)
                .param("planId", planId.value())
                .query(ITEM_ROW_MAPPER)
                .list();
    }
}
