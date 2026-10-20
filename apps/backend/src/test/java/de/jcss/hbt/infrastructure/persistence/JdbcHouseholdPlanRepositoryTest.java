package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.SubcategoryOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class JdbcHouseholdPlanRepositoryTest {

    private static final Year YEAR = Year.of(2026);
    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final ItemSection LIVING = ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT);

    @Autowired
    private JdbcHouseholdPlanRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM household_plan").update();
    }

    @Test
    void storesAndRestoresPlanWithAllItemAttributesInOrder() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        plan.addIncome(description("Gehalt"), money("2500.00"), null, null);
        plan.addExpense(description("Miete"), money("850.50"), ExpenseCategory.LEBENSUNTERHALT, null, null);

        repository.add(plan);

        HouseholdPlan restored = repository.findById(plan.id()).orElseThrow();
        assertThat(restored.owner()).isEqualTo(OWNER);
        assertThat(restored.year()).isEqualTo(YEAR);
        assertThat(restored.items())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(plan.items());
    }

    @Test
    void savingAgainAddsNewAndDeletesRemovedItems() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId kept = plan.addIncome(description("Gehalt"), money("100.00"), null, null);
        BudgetItemId removed = plan.addIncome(description("Bonus"), money("50.00"), null, null);
        repository.add(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.removeItem(removed);
        BudgetItemId added =
                loaded.addExpense(description("ETF"), money("20.00"), ExpenseCategory.SPAREN_INVESTIEREN, null, null);
        repository.save(loaded);

        assertThat(repository.findById(plan.id()).orElseThrow().items())
                .extracting(BudgetItem::id)
                .containsExactly(added, kept);
    }

    /** PB-4 */
    @Test
    void savingAChangedItemUpdatesItAndKeepsItsCreationTime() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId changed = plan.addExpense(description("Miete"), money("850.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        BudgetItemId later = plan.addIncome(description("Gehalt"), money("2500.00"), null, null);
        repository.add(plan);
        Instant createdAt = createdAtOf(changed);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.changeItem(changed, description("Warmmiete"), money("900.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);
        repository.save(loaded);

        HouseholdPlan restored = repository.findById(plan.id()).orElseThrow();
        assertThat(restored.items()).extracting(BudgetItem::id).containsExactly(changed, later);
        assertThat(restored.items().getFirst()).satisfies(item -> {
            assertThat(item.description()).isEqualTo(description("Warmmiete"));
            assertThat(item.amount()).isEqualTo(money("900.00"));
            assertThat(item.category()).contains(ExpenseCategory.FREIZEIT_SPASS);
        });
        assertThat(createdAtOf(changed)).isEqualTo(createdAt);
    }

    /** MJ-2, MJ-4 */
    @Test
    void storesTheEnteredAmountWithItsPeriodAndUpdatesAChangedPeriod() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        ItemAmount monthly = ItemAmount.monthly(Money.of(new BigDecimal("850.00")));
        BudgetItemId rent = plan.addExpense(description("Miete"), monthly, ExpenseCategory.LEBENSUNTERHALT, null, null);
        repository.add(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        assertThat(loaded.items().getFirst().amount()).isEqualTo(monthly);
        loaded.changeItem(rent, description("Miete"), money("850.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        repository.save(loaded);

        assertThat(repository.findById(plan.id()).orElseThrow().items().getFirst().amount())
                .isEqualTo(money("850.00"));
    }

    /** PN-1, PN-4 */
    @Test
    void storesAddedChangedAndRemovedNotes() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId rent = plan.addExpense(description("Miete"), money("850.00"), ExpenseCategory.LEBENSUNTERHALT,
                null,
                new BudgetItemNote("Warmmiete\ninkl. Nebenkosten"));
        BudgetItemId salary = plan.addIncome(description("Gehalt"), money("2500.00"), null, null);
        repository.add(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        assertThat(loaded.items()).extracting(BudgetItem::note)
                .containsExactly(Optional.empty(), Optional.of(new BudgetItemNote("Warmmiete\ninkl. Nebenkosten")));

        loaded.changeItem(rent, description("Miete"), money("850.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        loaded.changeItem(salary, description("Gehalt"), money("2500.00"), null, null, new BudgetItemNote("netto"));
        repository.save(loaded);

        assertThat(repository.findById(plan.id()).orElseThrow().items()).extracting(BudgetItem::note)
                .containsExactly(Optional.of(new BudgetItemNote("netto")), Optional.empty());
    }

    /** Saving writes only what a command changed, not every item of the plan again. */
    @Test
    void savingTakesTwoQueriesAndOneWriteHoweverManyItemsThePlanHas() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId first = null;
        for (int i = 0; i < 20; i++) {
            BudgetItemId id = plan.addIncome(description("Einnahme " + i), money("10.00"), null, null);
            first = first == null ? id : first;
        }
        repository.add(plan);
        AtomicInteger statements = new AtomicInteger();
        JdbcHouseholdPlanRepository counting =
                new JdbcHouseholdPlanRepository(JdbcClient.create(countingStatements(dataSource, statements)));

        HouseholdPlan loaded = counting.findById(plan.id()).orElseThrow();
        loaded.addIncome(description("Bonus"), money("5.00"), null, null);
        statements.set(0);
        counting.save(loaded);
        assertThat(statements).as("add").hasValue(3);

        loaded.changeItem(first, description("Gehalt"), money("10.00"), null, null, null);
        statements.set(0);
        counting.save(loaded);
        assertThat(statements).as("change").hasValue(3);

        loaded.removeItem(first);
        statements.set(0);
        counting.save(loaded);
        assertThat(statements).as("remove").hasValue(3);

        BudgetItemId bottom = loaded.items().getLast().id();
        loaded.changeItem(bottom, description("Gehalt"), money("10.00"), null, BudgetItemSubcategory.of("Gehalt"), null);
        statements.set(0);
        counting.save(loaded);
        assertThat(statements).as("move on top of another group").hasValue(3);

        statements.set(0);
        counting.save(loaded);
        assertThat(statements).as("nothing changed").hasValue(2);
        assertThat(repository.findById(plan.id()).orElseThrow().items()).hasSize(20);
    }

    /** Counts the statements prepared through connections of the data source. */
    private static DataSource countingStatements(DataSource dataSource, AtomicInteger statements) {
        return proxy(DataSource.class, dataSource, (method, result) -> method.getName().equals("getConnection")
                ? proxy(Connection.class, (Connection) result, (connectionMethod, connectionResult) -> {
                    if (connectionMethod.getName().equals("prepareStatement")) {
                        statements.incrementAndGet();
                    }
                    return connectionResult;
                })
                : result);
    }

    private static <T> T proxy(Class<T> type, T target, BiFunction<Method, Object, Object> afterCall) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            try {
                return afterCall.apply(method, method.invoke(target, args));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }));
    }

    /** UK-1, UK-4, UK-15 */
    @Test
    void storesSubcategoriesAndTheOrderOfItemsAndGroups() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId maintenance = expense(plan, "Wartung", "Auto");
        BudgetItemId tax = expense(plan, "Steuer", "Auto");
        BudgetItemId rent = expense(plan, "Kaltmiete", "Miete");
        BudgetItemId other = expense(plan, "Kleinkram", null);
        repository.add(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.reorderItems(List.of(maintenance, tax));
        loaded.reorderSubcategories(LIVING, List.of(BudgetItemSubcategory.OTHER, BudgetItemSubcategory.of("Miete"),
                BudgetItemSubcategory.of("Auto")));
        repository.save(loaded);

        HouseholdPlan restored = repository.findById(plan.id()).orElseThrow();
        assertThat(restored.items()).extracting(BudgetItem::id).containsExactly(other, rent, maintenance, tax);
        assertThat(restored.items()).extracting(item -> item.subcategory().map(BudgetItemSubcategory::value))
                .containsExactly(Optional.empty(), Optional.of("Miete"), Optional.of("Auto"), Optional.of("Auto"));
        assertThat(restored.subcategoryOrders()).isEqualTo(Map.of(LIVING, new SubcategoryOrder(List.of(
                BudgetItemSubcategory.OTHER, BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto")))));

        restored.removeItem(rent);
        repository.save(restored);

        assertThat(repository.findById(plan.id()).orElseThrow().subcategoryOrders()).isEqualTo(Map.of(LIVING,
                new SubcategoryOrder(List.of(BudgetItemSubcategory.OTHER, BudgetItemSubcategory.of("Auto")))));
    }

    /** UK-15 */
    @Test
    void ordersOfOnePlanDoNotAffectAnother() {
        HouseholdPlan first = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        HouseholdPlan second = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2027));
        for (HouseholdPlan plan : List.of(first, second)) {
            expense(plan, "Wartung", "Auto");
            expense(plan, "Kaltmiete", "Miete");
            repository.add(plan);
        }

        HouseholdPlan loaded = repository.findById(first.id()).orElseThrow();
        loaded.reorderSubcategories(LIVING, List.of(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto")));
        repository.save(loaded);

        assertThat(repository.findById(first.id()).orElseThrow().subcategoryOrders()).hasSize(1);
        assertThat(repository.findById(second.id()).orElseThrow().subcategoryOrders()).isEmpty();
    }

    /** PU-8, PU-9 */
    @Test
    void findsThePlanOfAnOwnerForAYear() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2031));
        repository.add(plan);

        assertThat(repository.findFor(OWNER, Year.of(2031))).map(HouseholdPlan::id).contains(plan.id());
        assertThat(repository.findFor(OWNER, Year.of(2032))).isEmpty();
        assertThat(repository.findFor(OwnerId.of(UUID.randomUUID()), Year.of(2031))).isEmpty();
    }

    @Test
    void removingTheLastItemLeavesAnEmptyPlan() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId itemId = plan.addIncome(description("Gehalt"), money("100.00"), null, null);
        repository.add(plan);

        plan.removeItem(itemId);
        repository.save(plan);

        assertThat(repository.findById(plan.id()).orElseThrow().items()).isEmpty();
    }

    /**
     * Without the lock taken by findById, the second transaction would load the plan
     * without the first one's item and delete it again when saving.
     */
    @Test
    void concurrentChangesToAPlanAreAppliedOneAfterTheOther() throws Exception {
        HouseholdPlanId planId = HouseholdPlanId.generate();
        repository.add(HouseholdPlan.create(planId, OWNER, YEAR));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        AtomicReference<Future<?>> second = new AtomicReference<>();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            transaction.executeWithoutResult(status -> {
                HouseholdPlan first = repository.findById(planId).orElseThrow();
                second.set(executor.submit(() -> transaction.executeWithoutResult(otherStatus -> {
                    HouseholdPlan other = repository.findById(planId).orElseThrow();
                    other.addIncome(description("Bonus"), money("50.00"), null, null);
                    repository.save(other);
                })));
                assertThatThrownBy(() -> second.get().get(500, TimeUnit.MILLISECONDS))
                        .as("second transaction waits for the first one")
                        .isInstanceOf(TimeoutException.class);
                first.addIncome(description("Gehalt"), money("100.00"), null, null);
                repository.save(first);
            });
            second.get().get(10, TimeUnit.SECONDS);
        }

        assertThat(repository.findById(planId).orElseThrow().items())
                .extracting(item -> item.description().value())
                .containsExactly("Bonus", "Gehalt");
    }

    @Test
    void doesNotFindUnknownPlan() {
        assertThat(repository.findById(HouseholdPlanId.generate())).isEmpty();
    }

    @Test
    void knowsWhetherAnOwnerHasAPlanForAYear() {
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR));

        assertThat(repository.existsFor(OWNER, YEAR)).isTrue();
        assertThat(repository.existsFor(OWNER, Year.of(2027))).isFalse();
        assertThat(repository.existsFor(OwnerId.of(UUID.randomUUID()), YEAR)).isFalse();
    }

    @Test
    void rejectsSecondPlanOfAnOwnerForTheSameYear() {
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR));

        assertThatThrownBy(() -> repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR)))
                .isInstanceOf(HouseholdPlanAlreadyExistsException.class);
    }

    @Test
    void differentOwnersMayHavePlansForTheSameYear() {
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR));
        HouseholdPlan otherOwners = HouseholdPlan.create(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()), YEAR);

        repository.add(otherOwners);

        assertThat(repository.findById(otherOwners.id())).isPresent();
    }

    private Instant createdAtOf(BudgetItemId itemId) {
        return jdbcClient.sql("SELECT created_at FROM budget_item WHERE id = :id")
                .param("id", itemId.value())
                .query((rs, rowNum) -> rs.getTimestamp("created_at").toInstant())
                .single();
    }

    private static BudgetItemId expense(HouseholdPlan plan, String description, String subcategory) {
        return plan.addExpense(description(description), money("10.00"), ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.ofNullable(subcategory), null);
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static ItemAmount money(String value) {
        return ItemAmount.yearly(Money.of(new BigDecimal(value)));
    }
}
