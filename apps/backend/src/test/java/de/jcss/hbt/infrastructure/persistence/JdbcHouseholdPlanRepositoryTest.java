package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class JdbcHouseholdPlanRepositoryTest {

    private static final Year YEAR = Year.of(2026);
    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());

    @Autowired
    private JdbcHouseholdPlanRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM household_plan").update();
    }

    @Test
    void storesAndRestoresPlanWithAllItemAttributesInOrder() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        plan.addIncome(description("Gehalt"), money("2500.00"));
        plan.addExpense(description("Miete"), money("850.50"), ExpenseCategory.LEBENSUNTERHALT);

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
        BudgetItemId kept = plan.addIncome(description("Gehalt"), money("100.00"));
        BudgetItemId removed = plan.addIncome(description("Bonus"), money("50.00"));
        repository.add(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.removeItem(removed);
        BudgetItemId added =
                loaded.addExpense(description("ETF"), money("20.00"), ExpenseCategory.SPAREN_INVESTIEREN);
        repository.save(loaded);

        assertThat(repository.findById(plan.id()).orElseThrow().items())
                .extracting(BudgetItem::id)
                .containsExactly(kept, added);
    }

    /** PB-4 */
    @Test
    void savingAChangedItemUpdatesItAndKeepsItsCreationTime() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId changed = plan.addExpense(description("Miete"), money("850.00"), ExpenseCategory.LEBENSUNTERHALT);
        BudgetItemId later = plan.addIncome(description("Gehalt"), money("2500.00"));
        repository.add(plan);
        Instant createdAt = createdAtOf(changed);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.changeItem(changed, description("Warmmiete"), money("900.00"), ExpenseCategory.FREIZEIT_SPASS);
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

    @Test
    void removingTheLastItemLeavesAnEmptyPlan() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, YEAR);
        BudgetItemId itemId = plan.addIncome(description("Gehalt"), money("100.00"));
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
                    other.addIncome(description("Bonus"), money("50.00"));
                    repository.save(other);
                })));
                assertThatThrownBy(() -> second.get().get(500, TimeUnit.MILLISECONDS))
                        .as("second transaction waits for the first one")
                        .isInstanceOf(TimeoutException.class);
                first.addIncome(description("Gehalt"), money("100.00"));
                repository.save(first);
            });
            second.get().get(10, TimeUnit.SECONDS);
        }

        assertThat(repository.findById(planId).orElseThrow().items())
                .extracting(item -> item.description().value())
                .containsExactly("Gehalt", "Bonus");
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

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
