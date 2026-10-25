package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.valueobject.AmountPeriod;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SubcategoryOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link HouseholdPlan} aggregates in the {@code household_plan} and
 * {@code budget_item} tables. Items are rebuilt through the domain factories, so stored
 * data passes the same invariants as new input. Expects to run within the transaction of
 * the calling application service.
 * <p>
 * The display order of the items is stored in {@code budget_item.position}, the highest on
 * top. Positions need not be consecutive: saving keeps every stored position that still fits the
 * order, so adding an item at the bottom, moving one there or removing one writes no other item; positions
 * below zero are fine. The
 * arranged order of the subcategory groups is stored in {@code subcategory_order}.
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
                new ItemAmount(Money.of(rs.getBigDecimal("amount")), AmountPeriod.valueOf(rs.getString("amount_period"))),
                ItemType.valueOf(rs.getString("type")),
                category == null ? null : ExpenseCategory.valueOf(category),
                BudgetItemSubcategory.ofNullable(rs.getString("subcategory")),
                BudgetItemNote.ofNullable(rs.getString("note")),
                ItemMarksColumns.of(rs));
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
                .map(row -> HouseholdPlan.reconstitute(id, row.owner(), row.year(), findItems(id),
                        ItemSectionColumn.subcategoryOrdersOf(jdbcClient, id)));
    }

    private record PlanRow(OwnerId owner, Year year) {
    }

    @Override
    public Optional<HouseholdPlan> findFor(OwnerId owner, Year year) {
        return jdbcClient.sql("SELECT id FROM household_plan WHERE owner_id = :owner AND year = :year FOR UPDATE")
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query((rs, rowNum) -> HouseholdPlanId.of(rs.getObject("id", UUID.class)))
                .optional()
                .flatMap(this::findById);
    }

    private List<BudgetItem> findItems(HouseholdPlanId planId) {
        return findStoredItems(planId).stream().map(StoredItem::item).toList();
    }

    private List<StoredItem> findStoredItems(HouseholdPlanId planId) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, amount_period, type, category, subcategory, note, important,
                               saving_potential, position
                        FROM budget_item
                        WHERE household_plan_id = :planId
                        ORDER BY position DESC
                        """)
                .param("planId", planId.value())
                .query((rs, rowNum) -> new StoredItem(ITEM_ROW_MAPPER.mapRow(rs, rowNum), rs.getInt("position")))
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
        List<BudgetItem> items = plan.items();
        List<Integer> positions = positionsOf(items, Map.of());
        for (int index = 0; index < items.size(); index++) {
            insertItem(plan.id(), items.get(index), positions.get(index));
        }
        insertSubcategoryOrders(plan.id(), plan.subcategoryOrders());
    }

    /** Items and the order of their groups go with the plan through {@code ON DELETE CASCADE}. */
    @Override
    public void remove(HouseholdPlan plan) {
        jdbcClient.sql("DELETE FROM household_plan WHERE id = :id")
                .param("id", plan.id().value())
                .update();
    }

    /**
     * Compares the items with the stored ones and writes only the differences: deletes the
     * removed items, inserts the new ones and updates those whose values or position changed.
     * As a command changes a single item, saving takes two queries and one write however many
     * items the plan has. The arranged order of the groups is only rewritten if it changed.
     */
    @Override
    public void save(HouseholdPlan plan) {
        Map<BudgetItemId, StoredItem> stored = new HashMap<>();
        findStoredItems(plan.id()).forEach(item -> stored.put(item.item().id(), item));
        List<BudgetItem> items = plan.items();
        Map<BudgetItemId, Integer> storedPositions = new HashMap<>();
        stored.forEach((id, item) -> storedPositions.put(id, item.position()));
        List<Integer> positions = positionsOf(items, storedPositions);
        for (int index = 0; index < items.size(); index++) {
            BudgetItem item = items.get(index);
            int position = positions.get(index);
            StoredItem storedItem = stored.remove(item.id());
            if (storedItem == null) {
                insertItem(plan.id(), item, position);
            } else if (!sameValues(item, storedItem.item()) || position != storedItem.position()) {
                updateItem(item, position);
            }
        }
        deleteItems(stored.keySet());
        Map<ItemSection, SubcategoryOrder> orders = plan.subcategoryOrders();
        if (!orders.equals(ItemSectionColumn.subcategoryOrdersOf(jdbcClient, plan.id()))) {
            jdbcClient.sql("DELETE FROM subcategory_order WHERE household_plan_id = :planId")
                    .param("planId", plan.id().value())
                    .update();
            insertSubcategoryOrders(plan.id(), orders);
        }
    }

    private record StoredItem(BudgetItem item, int position) {
    }

    /**
     * The positions of the items in display order, falling from the top down. Each item keeps its
     * stored position as long as it lies below that of the item above; otherwise it gets the next
     * position below. Without stored positions the bottom item gets 0.
     */
    private static List<Integer> positionsOf(List<BudgetItem> items, Map<BudgetItemId, Integer> storedPositions) {
        Integer[] positions = new Integer[items.size()];
        int above = storedPositions.values().stream().max(Integer::compare).map(max -> max + 1).orElse(items.size());
        for (int index = 0; index < items.size(); index++) {
            Integer stored = storedPositions.get(items.get(index).id());
            above = stored != null && stored < above ? stored : above - 1;
            positions[index] = above;
        }
        return List.of(positions);
    }

    private static boolean sameValues(BudgetItem item, BudgetItem other) {
        return item.description().equals(other.description()) && item.amount().equals(other.amount())
                && item.category().equals(other.category()) && item.subcategory().equals(other.subcategory())
                && item.note().equals(other.note()) && item.marks().equals(other.marks());
    }

    private void insertItem(HouseholdPlanId planId, BudgetItem item, int position) {
        jdbcClient.sql("""
                        INSERT INTO budget_item (id, household_plan_id, description, amount, amount_period, type,
                                                 category, subcategory, note, important, saving_potential, position)
                        VALUES (:id, :planId, :description, :amount, :amountPeriod, :type, :category, :subcategory,
                                :note, :important, :savingPotential, :position)
                        """)
                .param("id", item.id().value())
                .param("planId", planId.value())
                .param("description", item.description().value())
                .param("amount", item.amount().value().value())
                .param("amountPeriod", item.amount().period().name())
                .param("type", item.type().name())
                .param("category", categoryOf(item))
                .param("subcategory", subcategoryOf(item))
                .param("note", noteOf(item))
                .param("important", item.marks().important())
                .param("savingPotential", ItemMarksColumns.savingPotentialOf(item.marks()))
                .param("position", position)
                .update();
    }

    /** The type of an item never changes, see {@link BudgetItem}. */
    private void updateItem(BudgetItem item, int position) {
        jdbcClient.sql("""
                        UPDATE budget_item
                        SET description = :description, amount = :amount, amount_period = :amountPeriod, category = :category,
                            subcategory = :subcategory, note = :note, important = :important,
                            saving_potential = :savingPotential, position = :position
                        WHERE id = :id
                        """)
                .param("id", item.id().value())
                .param("description", item.description().value())
                .param("amount", item.amount().value().value())
                .param("amountPeriod", item.amount().period().name())
                .param("category", categoryOf(item))
                .param("subcategory", subcategoryOf(item))
                .param("note", noteOf(item))
                .param("important", item.marks().important())
                .param("savingPotential", ItemMarksColumns.savingPotentialOf(item.marks()))
                .param("position", position)
                .update();
    }

    private void insertSubcategoryOrders(HouseholdPlanId planId, Map<ItemSection, SubcategoryOrder> orders) {
        orders.forEach((section, order) -> {
            List<BudgetItemSubcategory> subcategories = order.subcategories();
            for (int position = 0; position < subcategories.size(); position++) {
                jdbcClient.sql("""
                                INSERT INTO subcategory_order (household_plan_id, section, position, subcategory)
                                VALUES (:planId, :section, :position, :subcategory)
                                """)
                        .param("planId", planId.value())
                        .param("section", ItemSectionColumn.of(section))
                        .param("position", position)
                        .param("subcategory", subcategories.get(position).value())
                        .update();
            }
        });
    }

    private void deleteItems(Collection<BudgetItemId> ids) {
        if (ids.isEmpty()) {
            return;
        }
        jdbcClient.sql("DELETE FROM budget_item WHERE id IN (:ids)")
                .param("ids", ids.stream().map(BudgetItemId::value).toList())
                .update();
    }

    private static String categoryOf(BudgetItem item) {
        return item.category().map(Enum::name).orElse(null);
    }

    private static String subcategoryOf(BudgetItem item) {
        return item.subcategory().map(BudgetItemSubcategory::value).orElse(null);
    }

    private static String noteOf(BudgetItem item) {
        return item.note().map(BudgetItemNote::value).orElse(null);
    }
}
