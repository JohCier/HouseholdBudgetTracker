package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.event.BudgetItemChanged;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link HouseholdPlanApplicationService} loads, saves and publishes
 * correctly, using a simple in-memory fake repository instead of a database and
 * capturing the published events. The overview query handler is faked by deriving the
 * read model from the stored plans.
 */
class HouseholdPlanApplicationServiceTest {

    private static final Year YEAR = Year.of(2026);
    private static final BudgetItemDescription SALARY = BudgetItemDescription.of("Gehalt");
    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final OwnerId SOMEONE_ELSE = OwnerId.of(UUID.randomUUID());

    private final FakeRepository repository = new FakeRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final HouseholdPlanApplicationService service =
            new HouseholdPlanApplicationService(repository, repository::overviewOf, publishedEvents::add);

    private HouseholdPlanId planId;

    @BeforeEach
    void createPlan() {
        planId = service.createPlan(new CreateHouseholdPlanCommand(OWNER, YEAR));
    }

    @Test
    void createPlanStoresItAndPublishesCreation() {
        assertThat(repository.plans.get(planId).year()).isEqualTo(YEAR);
        assertThat(repository.plans.get(planId).isOwnedBy(OWNER)).isTrue();
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(HouseholdPlanCreated.class, event -> {
                    assertThat(event.planId()).isEqualTo(planId);
                    assertThat(event.owner()).isEqualTo(OWNER);
                    assertThat(event.year()).isEqualTo(YEAR);
                });
    }

    @Test
    void onlyOnePlanPerOwnerAndYear() {
        assertThatThrownBy(() -> service.createPlan(new CreateHouseholdPlanCommand(OWNER, YEAR)))
                .isInstanceOf(HouseholdPlanAlreadyExistsException.class);
        assertThat(repository.plans).hasSize(1);
    }

    @Test
    void otherOwnersHavePlansOfTheirOwnForTheSameYear() {
        HouseholdPlanId otherPlan = service.createPlan(new CreateHouseholdPlanCommand(SOMEONE_ELSE, YEAR));

        assertThat(otherPlan).isNotEqualTo(planId);
        assertThat(service.getOverview(SOMEONE_ELSE, YEAR).planId()).isEqualTo(otherPlan);
        assertThat(service.getOverview(OWNER, YEAR).planId()).isEqualTo(planId);
    }

    @Test
    void plansOfOtherOwnersCannotBeChangedAndAppearNotToExist() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, money("2000.00")));
        publishedEvents.clear();

        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand(SOMEONE_ELSE, planId, SALARY, money("1.00"))))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
        assertThatThrownBy(() -> service.addExpense(new AddExpenseCommand(SOMEONE_ELSE, planId, SALARY,
                money("1.00"), ExpenseCategory.FREIZEIT_SPASS)))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
        assertThatThrownBy(() -> service.changeItem(new ChangeBudgetItemCommand(SOMEONE_ELSE, planId, itemId, SALARY,
                money("1.00"), null)))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
        assertThatThrownBy(() -> service.removeItem(new RemoveBudgetItemCommand(SOMEONE_ELSE, planId, itemId)))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
        assertThat(storedItems()).extracting(BudgetItem::id).containsExactly(itemId);
        assertThat(storedItems().getFirst().amount()).isEqualTo(money("2000.00"));
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void plansOfOtherOwnersAreNotVisible() {
        assertThatThrownBy(() -> service.getOverview(SOMEONE_ELSE, YEAR))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
    }

    @Test
    void addIncomeStoresItemAndPublishesEvent() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, money("2000.00")));

        assertThat(storedItems()).extracting(BudgetItem::id).containsExactly(itemId);
        assertThat(publishedEvents.getLast()).isInstanceOfSatisfying(IncomeAdded.class,
                event -> assertThat(event.itemId()).isEqualTo(itemId));
    }

    @Test
    void addExpenseStoresItemAndPublishesEvent() {
        BudgetItemId itemId = service.addExpense(new AddExpenseCommand(
                OWNER, planId, BudgetItemDescription.of("Miete"), money("800.00"), ExpenseCategory.LEBENSUNTERHALT));

        assertThat(storedItems().getFirst().category()).contains(ExpenseCategory.LEBENSUNTERHALT);
        assertThat(publishedEvents.getLast()).isInstanceOfSatisfying(ExpenseAdded.class,
                event -> assertThat(event.itemId()).isEqualTo(itemId));
    }

    @Test
    void nonPositiveAmountIsADomainErrorAndPublishesNothing() {
        publishedEvents.clear();

        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, Money.ZERO)))
                .isInstanceOf(InvalidBudgetItemException.class);
        assertThat(storedItems()).isEmpty();
        assertThat(publishedEvents).isEmpty();
    }

    /** PB-1, PB-5 */
    @Test
    void changeItemStoresItAndPublishesEvent() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, money("2000.00")));

        service.changeItem(new ChangeBudgetItemCommand(OWNER, planId, itemId, SALARY, money("2100.00"), null));

        assertThat(storedItems()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(itemId);
            assertThat(item.amount()).isEqualTo(money("2100.00"));
        });
        assertThat(publishedEvents.getLast()).isInstanceOfSatisfying(BudgetItemChanged.class,
                event -> assertThat(event.itemId()).isEqualTo(itemId));
    }

    /** PB-6 */
    @Test
    void changingUnknownItemThrows() {
        assertThatThrownBy(() -> service.changeItem(new ChangeBudgetItemCommand(OWNER, planId, BudgetItemId.generate(),
                SALARY, money("1.00"), null)))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    @Test
    void removeItemDeletesItAndPublishesEvent() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, money("2000.00")));

        service.removeItem(new RemoveBudgetItemCommand(OWNER, planId, itemId));

        assertThat(storedItems()).isEmpty();
        assertThat(publishedEvents.getLast()).isInstanceOfSatisfying(BudgetItemRemoved.class,
                event -> assertThat(event.itemId()).isEqualTo(itemId));
    }

    @Test
    void removingUnknownItemThrows() {
        assertThatThrownBy(() -> service.removeItem(new RemoveBudgetItemCommand(OWNER, planId, BudgetItemId.generate())))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    @Test
    void commandsForUnknownPlanThrow() {
        HouseholdPlanId unknown = HouseholdPlanId.generate();

        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand(OWNER, unknown, SALARY, money("10.00"))))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
    }

    @Test
    void overviewOfAYearContainsThePlanAndItsItems() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(OWNER, planId, SALARY, money("2000.00")));

        HouseholdPlanOverview overview = service.getOverview(OWNER, YEAR);

        assertThat(overview.planId()).isEqualTo(planId);
        assertThat(overview.items()).extracting(OverviewItem::id).containsExactly(itemId);
        assertThat(overview.totalIncome().value()).isEqualByComparingTo("2000.00");
    }

    @Test
    void overviewOfAYearWithoutPlanThrows() {
        assertThatThrownBy(() -> service.getOverview(OWNER, Year.of(2030)))
                .isInstanceOf(HouseholdPlanNotFoundException.class)
                .hasMessage("No household plan for 2030");
    }

    private List<BudgetItem> storedItems() {
        return repository.plans.get(planId).items();
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    /** Stores plans by rebuilding them, as a real repository would, so no state leaks between calls. */
    private static final class FakeRepository implements HouseholdPlanRepository {

        private final Map<HouseholdPlanId, HouseholdPlan> plans = new HashMap<>();

        @Override
        public Optional<HouseholdPlan> findById(HouseholdPlanId id) {
            return Optional.ofNullable(plans.get(id))
                    .map(plan -> HouseholdPlan.reconstitute(plan.id(), plan.owner(), plan.year(), plan.items()));
        }

        @Override
        public boolean existsFor(OwnerId owner, Year year) {
            return plans.values().stream().anyMatch(plan -> plan.isOwnedBy(owner) && plan.year().equals(year));
        }

        @Override
        public void add(HouseholdPlan plan) {
            if (existsFor(plan.owner(), plan.year())) {
                throw new HouseholdPlanAlreadyExistsException(plan.year());
            }
            save(plan);
        }

        @Override
        public void save(HouseholdPlan plan) {
            plans.put(plan.id(),
                    HouseholdPlan.reconstitute(plan.id(), plan.owner(), plan.year(), List.copyOf(plan.items())));
        }

        Optional<HouseholdPlanOverview> overviewOf(OwnerId owner, Year year) {
            return plans.values().stream()
                    .filter(plan -> plan.isOwnedBy(owner) && plan.year().equals(year))
                    .findFirst()
                    .map(plan -> HouseholdPlanOverview.of(plan.id(), plan.year(), plan.items().stream()
                            .map(item -> new OverviewItem(item.id(), item.description(), item.amount(),
                                    item.type(), item.category().orElse(null)))
                            .toList()));
        }
    }
}
