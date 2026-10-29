package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.overview.DeletedPlanEntry;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlans;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the overviews straight from the tables, without reconstituting the
 * {@code HouseholdPlan} aggregate. Items are listed in display order, the highest position on
 * top (UK-14), see {@link JdbcHouseholdPlanRepository}. Only the deleted plans are read from rows
 * with {@code deleted_at}; all other overviews skip them (GP-3).
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
        return jdbcClient.sql("""
                        SELECT id FROM household_plan
                        WHERE owner_id = :owner AND year = :year AND deleted_at IS NULL
                        """)
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query((rs, rowNum) -> HouseholdPlanId.of(rs.getObject("id", UUID.class)))
                .optional()
                .map(planId -> HouseholdPlanOverview.of(planId, year, itemsOf(planId),
                        ItemSectionColumn.subcategoryOrdersOf(jdbcClient, planId)));
    }

    @Override
    public YearlyOverview findYearlyOverviewByOwner(OwnerId owner, Year firstYear, Year lastYear) {
        List<YearlyOverview.PlanSummary> active = summarize(owner, firstYear, lastYear, false).stream()
                .map(plan -> new YearlyOverview.PlanSummary(plan.planId(), plan.year(), plan.summary()))
                .toList();
        int deletedPlanCount = jdbcClient.sql("""
                        SELECT COUNT(*) FROM household_plan
                        WHERE owner_id = :owner AND year BETWEEN :firstYear AND :lastYear AND deleted_at IS NOT NULL
                        """)
                .param("owner", owner.value())
                .param("firstYear", firstYear.getValue())
                .param("lastYear", lastYear.getValue())
                .query(Integer.class)
                .single();
        return YearlyOverview.of(active, oneTimeSummariesOf(owner, firstYear, lastYear))
                .withDeletedPlanCount(deletedPlanCount);
    }

    @Override
    public DeletedPlans findDeletedPlansByOwner(OwnerId owner, Year firstYear, Year lastYear) {
        return new DeletedPlans(summarize(owner, firstYear, lastYear, true).stream()
                .map(plan -> DeletedPlanEntry.of(plan.planId(), plan.year(), plan.deletedAt(), plan.itemCount(),
                        plan.summary()))
                .toList());
    }

    /** A plan with when it was deleted ({@code null} if active), the number and the summary of its items. */
    private record StoredPlan(HouseholdPlanId planId, Year year, Instant deletedAt, int itemCount,
                              BudgetSummary summary) {
    }

    /** The active or the deleted plans of the owner in the range of years, each with the summary of its items. */
    private List<StoredPlan> summarize(OwnerId owner, Year firstYear, Year lastYear, boolean deleted) {
        Map<HouseholdPlanId, StoredPlan> plans = new LinkedHashMap<>();
        jdbcClient.sql("""
                        SELECT p.id, p.year, p.deleted_at, i.type, i.category, i.amount_period,
                               SUM(i.amount) AS amount, COUNT(i.id) AS item_count
                        FROM household_plan p
                        LEFT JOIN budget_item i ON i.household_plan_id = p.id
                        WHERE p.owner_id = :owner AND p.year BETWEEN :firstYear AND :lastYear
                          AND (p.deleted_at IS NOT NULL) = :deleted
                        GROUP BY p.id, p.year, p.deleted_at, i.type, i.category, i.amount_period
                        """)
                .param("owner", owner.value())
                .param("firstYear", firstYear.getValue())
                .param("lastYear", lastYear.getValue())
                .param("deleted", deleted)
                .query(rs -> {
                    HouseholdPlanId planId = HouseholdPlanId.of(rs.getObject("id", UUID.class));
                    OffsetDateTime deletedAt = rs.getObject("deleted_at", OffsetDateTime.class);
                    StoredPlan plan = plans.getOrDefault(planId, new StoredPlan(planId, Year.of(rs.getInt("year")),
                            deletedAt == null ? null : deletedAt.toInstant(), 0, BudgetSummary.EMPTY));
                    BudgetSummary summary = plan.summary();
                    String type = rs.getString("type");
                    if (type != null) {
                        // Monthly amounts count twelve times a year (MJ-8)
                        Money amount = new ItemAmount(Money.of(rs.getBigDecimal("amount")),
                                AmountPeriod.valueOf(rs.getString("amount_period"))).annual();
                        summary = ItemType.valueOf(type) == ItemType.INCOME
                                ? summary.withIncome(amount)
                                : summary.withExpense(ExpenseCategory.valueOf(rs.getString("category")), amount);
                    }
                    plans.put(planId, new StoredPlan(planId, plan.year(), plan.deletedAt(),
                            plan.itemCount() + rs.getInt("item_count"), summary));
                });
        return List.copyOf(plans.values());
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
