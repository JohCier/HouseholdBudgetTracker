package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link HouseholdPlan} aggregates in the {@code household_plan} and
 * {@code budget_item} tables. Items are rebuilt through the domain factories, so stored
 * data passes the same invariants as new input. Expects to run within the transaction of
 * the calling application service.
 * <p>
 * Concurrent changes are prevented by locking the plan row in {@link #findById}
 * ({@code SELECT ... FOR UPDATE}) until the transaction ends: a second transaction
 * loading the same plan waits and then sees the stored changes, so none get lost.
 */
@Repository
public class JdbcHouseholdPlanRepository implements HouseholdPlanRepository {

    private static final RowMapper<BudgetItem> ITEM_ROW_MAPPER = (rs, rowNum) -> {
        String category = rs.getString("category");
        return BudgetItem.reconstitute(
                BudgetItemId.of(rs.getObject("id", UUID.class)),
                BudgetItemDescription.of(rs.getString("description")),
                Money.of(rs.getBigDecimal("amount")),
                ItemType.valueOf(rs.getString("type")),
                category == null ? null : ExpenseCategory.valueOf(category),
                BudgetItemNote.ofNullable(rs.getString("note")));
    };

    private final JdbcClient jdbcClient;

    public JdbcHouseholdPlanRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<HouseholdPlan> findById(HouseholdPlanId id) {
        return jdbcClient.sql("SELECT owner_id, year FROM household_plan WHERE id = :id FOR UPDATE")
                .param("id", id.value())
                .query((rs, rowNum) -> new PlanRow(OwnerId.of(rs.getObject("owner_id", UUID.class)),
                        Year.of(rs.getInt("year"))))
                .optional()
                .map(row -> HouseholdPlan.reconstitute(id, row.owner(), row.year(), findItems(id)));
    }

    private record PlanRow(OwnerId owner, Year year) {
    }

    private List<BudgetItem> findItems(HouseholdPlanId planId) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, type, category, note
                        FROM budget_item
                        WHERE household_plan_id = :planId
                        ORDER BY created_at, id
                        """)
                .param("planId", planId.value())
                .query(ITEM_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean existsFor(OwnerId owner, Year year) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM household_plan WHERE owner_id = :owner AND year = :year)")
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query(Boolean.class)
                .single();
    }

    @Override
    public void add(HouseholdPlan plan) {
        try {
            jdbcClient.sql("INSERT INTO household_plan (id, owner_id, year) VALUES (:id, :owner, :year)")
                    .param("id", plan.id().value())
                    .param("owner", plan.owner().value())
                    .param("year", plan.year().getValue())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new HouseholdPlanAlreadyExistsException(plan.year());
        }
        plan.items().forEach(item -> upsertItem(plan.id(), item));
    }

    /**
     * Deletes the removed items, inserts the new ones and updates the changed ones. The
     * creation time of an existing item is kept, so a changed item keeps its position.
     */
    @Override
    public void save(HouseholdPlan plan) {
        deleteRemovedItems(plan);
        plan.items().forEach(item -> upsertItem(plan.id(), item));
    }

    private void deleteRemovedItems(HouseholdPlan plan) {
        List<UUID> keptIds = plan.items().stream().map(item -> item.id().value()).toList();
        if (keptIds.isEmpty()) {
            jdbcClient.sql("DELETE FROM budget_item WHERE household_plan_id = :planId")
                    .param("planId", plan.id().value())
                    .update();
            return;
        }
        jdbcClient.sql("DELETE FROM budget_item WHERE household_plan_id = :planId AND id NOT IN (:keptIds)")
                .param("planId", plan.id().value())
                .param("keptIds", keptIds)
                .update();
    }

    /** Unchanged items are left alone, so only new and changed items are written. */
    private void upsertItem(HouseholdPlanId planId, BudgetItem item) {
        jdbcClient.sql("""
                        INSERT INTO budget_item (id, household_plan_id, description, amount, type, category, note)
                        VALUES (:id, :planId, :description, :amount, :type, :category, :note)
                        ON CONFLICT (id) DO UPDATE
                        SET description = EXCLUDED.description, amount = EXCLUDED.amount, category = EXCLUDED.category,
                            note = EXCLUDED.note
                        WHERE (budget_item.description, budget_item.amount, budget_item.category, budget_item.note)
                              IS DISTINCT FROM (EXCLUDED.description, EXCLUDED.amount, EXCLUDED.category, EXCLUDED.note)
                        """)
                .param("id", item.id().value())
                .param("planId", planId.value())
                .param("description", item.description().value())
                .param("amount", item.amount().value())
                .param("type", item.type().name())
                .param("category", item.category().map(Enum::name).orElse(null))
                .param("note", item.note().map(BudgetItemNote::value).orElse(null))
                .update();
    }
}
