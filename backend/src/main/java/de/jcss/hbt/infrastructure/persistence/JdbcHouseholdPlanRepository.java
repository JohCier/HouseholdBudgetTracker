package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
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
 */
@Repository
public class JdbcHouseholdPlanRepository implements HouseholdPlanRepository {

    private static final RowMapper<BudgetItem> ITEM_ROW_MAPPER = (rs, rowNum) -> {
        BudgetItemId id = BudgetItemId.of(rs.getObject("id", UUID.class));
        String description = rs.getString("description");
        Money amount = Money.of(rs.getBigDecimal("amount"));
        return switch (ItemType.valueOf(rs.getString("type"))) {
            case INCOME -> BudgetItem.income(id, description, amount);
            case EXPENSE -> BudgetItem.expense(id, description, amount,
                    ExpenseCategory.valueOf(rs.getString("category")));
        };
    };

    private final JdbcClient jdbcClient;

    public JdbcHouseholdPlanRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<HouseholdPlan> findById(HouseholdPlanId id) {
        return jdbcClient.sql("SELECT year FROM household_plan WHERE id = :id")
                .param("id", id.value())
                .query(Integer.class)
                .optional()
                .map(year -> HouseholdPlan.reconstitute(id, Year.of(year), findItems(id)));
    }

    private List<BudgetItem> findItems(HouseholdPlanId planId) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, type, category
                        FROM budget_item
                        WHERE household_plan_id = :planId
                        ORDER BY created_at, id
                        """)
                .param("planId", planId.value())
                .query(ITEM_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean existsForYear(Year year) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM household_plan WHERE year = :year)")
                .param("year", year.getValue())
                .query(Boolean.class)
                .single();
    }

    /**
     * Items are immutable, so bringing them up to date only means deleting the removed
     * ones and inserting the new ones.
     */
    @Override
    public void save(HouseholdPlan plan) {
        insertPlanIfNew(plan);
        deleteRemovedItems(plan);
        plan.items().forEach(item -> insertItemIfNew(plan.id(), item));
    }

    private void insertPlanIfNew(HouseholdPlan plan) {
        try {
            jdbcClient.sql("INSERT INTO household_plan (id, year) VALUES (:id, :year) ON CONFLICT (id) DO NOTHING")
                    .param("id", plan.id().value())
                    .param("year", plan.year().getValue())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new HouseholdPlanAlreadyExistsException(plan.year());
        }
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

    private void insertItemIfNew(HouseholdPlanId planId, BudgetItem item) {
        jdbcClient.sql("""
                        INSERT INTO budget_item (id, household_plan_id, description, amount, type, category)
                        VALUES (:id, :planId, :description, :amount, :type, :category)
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param("id", item.id().value())
                .param("planId", planId.value())
                .param("description", item.description())
                .param("amount", item.amount().value())
                .param("type", item.type().name())
                .param("category", item.category().map(Enum::name).orElse(null))
                .update();
    }
}
