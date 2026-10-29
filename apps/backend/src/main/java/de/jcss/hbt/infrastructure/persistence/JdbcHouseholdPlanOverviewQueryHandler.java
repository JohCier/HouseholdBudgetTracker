package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import de.jcss.hbt.domain.householdplan.valueobject.AmountPeriod;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the overviews straight from the tables, without reconstituting the
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
                BudgetItemNote.ofNullable(rs.getString("note")),
                rs.getBoolean("important"),
                SavingPotentialColumn.of(rs));
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

    @Override
    public YearlyOverview findYearlyOverviewByOwner(OwnerId owner, Year firstYear, Year lastYear) {
        Map<HouseholdPlanId, Year> years = new LinkedHashMap<>();
        Map<HouseholdPlanId, BudgetSummary> summaries = new HashMap<>();
        jdbcClient.sql("""
                        SELECT p.id, p.year, i.type, i.category, i.amount_period, SUM(i.amount) AS amount
                        FROM household_plan p
                        LEFT JOIN budget_item i ON i.household_plan_id = p.id
                        WHERE p.owner_id = :owner AND p.year BETWEEN :firstYear AND :lastYear
                        GROUP BY p.id, p.year, i.type, i.category, i.amount_period
                        """)
                .param("owner", owner.value())
                .param("firstYear", firstYear.getValue())
                .param("lastYear", lastYear.getValue())
                .query(rs -> {
                    HouseholdPlanId planId = HouseholdPlanId.of(rs.getObject("id", UUID.class));
                    years.put(planId, Year.of(rs.getInt("year")));
                    BudgetSummary summary = summaries.getOrDefault(planId, BudgetSummary.EMPTY);
                    String type = rs.getString("type");
                    if (type != null) {
                        // Monthly amounts count twelve times a year (MJ-8)
                        Money amount = new ItemAmount(Money.of(rs.getBigDecimal("amount")),
                                AmountPeriod.valueOf(rs.getString("amount_period"))).annual();
                        summary = ItemType.valueOf(type) == ItemType.INCOME
                                ? summary.withIncome(amount)
                                : summary.withExpense(ExpenseCategory.valueOf(rs.getString("category")), amount);
                    }
                    summaries.put(planId, summary);
                });
        return YearlyOverview.of(years.entrySet().stream()
                        .map(plan -> new YearlyOverview.PlanSummary(plan.getKey(), plan.getValue(), summaries.get(plan.getKey())))
                        .toList(),
                oneTimeSummariesOf(owner, firstYear, lastYear));
    }

    /** The totals of the one-time items per year, only years that have some (EP-12, EP-13). */
    private List<YearlyOverview.OneTimeSummary> oneTimeSummariesOf(OwnerId owner, Year firstYear, Year lastYear) {
        return jdbcClient.sql("""
                        SELECT CAST(EXTRACT(YEAR FROM item_date) AS INTEGER) AS year,
                               COALESCE(SUM(amount) FILTER (WHERE type = 'INCOME'), 0) AS income,
                               COALESCE(SUM(amount) FILTER (WHERE type = 'EXPENSE'), 0) AS expense
                        FROM one_time_item
                        WHERE owner_id = :owner AND item_date BETWEEN :first AND :last
                        GROUP BY 1
                        """)
                .param("owner", owner.value())
                .param("first", firstYear.atDay(1))
                .param("last", lastYear.atMonth(12).atEndOfMonth())
                .query((rs, rowNum) -> new YearlyOverview.OneTimeSummary(Year.of(rs.getInt("year")),
                        Money.of(rs.getBigDecimal("income")), Money.of(rs.getBigDecimal("expense"))))
                .list();
    }

    private List<OverviewItem> itemsOf(HouseholdPlanId planId) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, amount_period, type, category, subcategory, note, important,
                               saving_potential
                        FROM budget_item
                        WHERE household_plan_id = :planId
                        ORDER BY position DESC
                        """)
                .param("planId", planId.value())
                .query(ITEM_ROW_MAPPER)
                .list();
    }
}
