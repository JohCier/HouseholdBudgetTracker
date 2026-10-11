package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.application.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.application.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.application.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.application.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import de.jcss.hbt.domain.householdplan.event.ExpenseAdded;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.event.IncomeAdded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final FakeRepository repository = new FakeRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final HouseholdPlanApplicationService service =
            new HouseholdPlanApplicationService(repository, repository::overviewOf, publishedEvents::add);

    private HouseholdPlanId planId;

    @BeforeEach
    void createPlan() {
        planId = service.createPlan(new CreateHouseholdPlanCommand(YEAR));
    }

    @Test
    void createPlanStoresItAndPublishesCreation() {
        assertThat(repository.plans.get(planId).year()).isEqualTo(YEAR);
        assertThat(publishedEvents).containsExactly(new HouseholdPlanCreated(planId, YEAR));
    }

    @Test
    void onlyOnePlanPerYear() {
        assertThatThrownBy(() -> service.createPlan(new CreateHouseholdPlanCommand(YEAR)))
                .isInstanceOf(HouseholdPlanAlreadyExistsException.class);
        assertThat(repository.plans).hasSize(1);
    }

    @Test
    void addIncomeStoresItemAndPublishesEvent() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(planId, "Gehalt", new BigDecimal("2000.00")));

        assertThat(storedItems()).extracting(BudgetItem::id).containsExactly(itemId);
        assertThat(publishedEvents).endsWith(new IncomeAdded(planId, itemId));
    }

    @Test
    void addExpenseStoresItemAndPublishesEvent() {
        BudgetItemId itemId = service.addExpense(new AddExpenseCommand(
                planId, "Miete", new BigDecimal("800.00"), ExpenseCategory.LEBENSUNTERHALT));

        assertThat(storedItems().getFirst().category()).contains(ExpenseCategory.LEBENSUNTERHALT);
        assertThat(publishedEvents).endsWith(new ExpenseAdded(planId, itemId));
    }

    @Test
    void missingAmountIsADomainErrorAndPublishesNothing() {
        publishedEvents.clear();

        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand(planId, "Gehalt", null)))
                .isInstanceOf(InvalidBudgetItemException.class);
        assertThat(storedItems()).isEmpty();
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void removeItemDeletesItAndPublishesEvent() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(planId, "Gehalt", new BigDecimal("2000.00")));

        service.removeItem(new RemoveBudgetItemCommand(planId, itemId));

        assertThat(storedItems()).isEmpty();
        assertThat(publishedEvents).endsWith(new BudgetItemRemoved(planId, itemId));
    }

    @Test
    void removingUnknownItemThrows() {
        assertThatThrownBy(() -> service.removeItem(new RemoveBudgetItemCommand(planId, BudgetItemId.generate())))
                .isInstanceOf(BudgetItemNotFoundException.class);
    }

    @Test
    void commandsForUnknownPlanThrow() {
        HouseholdPlanId unknown = HouseholdPlanId.generate();

        assertThatThrownBy(() -> service.addIncome(new AddIncomeCommand(unknown, "Gehalt", BigDecimal.TEN)))
                .isInstanceOf(HouseholdPlanNotFoundException.class);
    }

    @Test
    void overviewOfAYearContainsThePlanAndItsItems() {
        BudgetItemId itemId = service.addIncome(new AddIncomeCommand(planId, "Gehalt", new BigDecimal("2000.00")));

        HouseholdPlanOverview overview = service.getOverview(YEAR);

        assertThat(overview.planId()).isEqualTo(planId);
        assertThat(overview.items()).extracting(OverviewItem::id).containsExactly(itemId);
        assertThat(overview.totalIncome().value()).isEqualByComparingTo("2000.00");
    }

    @Test
    void overviewOfAYearWithoutPlanThrows() {
        assertThatThrownBy(() -> service.getOverview(Year.of(2030)))
                .isInstanceOf(HouseholdPlanNotFoundException.class)
                .hasMessage("No household plan for 2030");
    }

    private List<BudgetItem> storedItems() {
        return repository.plans.get(planId).items();
    }

    /** Stores plans by rebuilding them, as a real repository would, so no state leaks between calls. */
    private static final class FakeRepository implements HouseholdPlanRepository {

        private final Map<HouseholdPlanId, HouseholdPlan> plans = new HashMap<>();

        @Override
        public Optional<HouseholdPlan> findById(HouseholdPlanId id) {
            return Optional.ofNullable(plans.get(id))
                    .map(plan -> HouseholdPlan.reconstitute(plan.id(), plan.year(), plan.items()));
        }

        @Override
        public boolean existsForYear(Year year) {
            return plans.values().stream().anyMatch(plan -> plan.year().equals(year));
        }

        @Override
        public void save(HouseholdPlan plan) {
            plans.put(plan.id(), HouseholdPlan.reconstitute(plan.id(), plan.year(), List.copyOf(plan.items())));
        }

        Optional<HouseholdPlanOverview> overviewOf(Year year) {
            return plans.values().stream()
                    .filter(plan -> plan.year().equals(year))
                    .findFirst()
                    .map(plan -> HouseholdPlanOverview.of(plan.id(), plan.year(), plan.items().stream()
                            .map(item -> new OverviewItem(item.id(), item.description(), item.amount(),
                                    item.type(), item.category().orElse(null)))
                            .toList()));
        }
    }
}
