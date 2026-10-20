package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderBudgetItemsCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderSubcategoriesCommand;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Objects;

/**
 * Entry point for all use cases of household plans.
 * <p>
 * Every use case acts on behalf of a user, the owner given in the command. Users only
 * ever get to see their own plans: a plan of someone else is treated exactly like one
 * that does not exist, so its existence is not revealed either. Whether a plan belongs
 * to a user is decided by the plan itself ({@link HouseholdPlan#isOwnedBy}).
 * <p>
 * Write side: each command runs in its own transaction:
 * load the {@link HouseholdPlan}, let it apply the change, save it, and publish the
 * domain events it recorded. Every command answers with the overview of the plan after
 * the change (AU-1, AU-2), read within the same transaction so it includes the change. Events are published through Spring's
 * {@link ApplicationEventPublisher}, so listeners can choose to run within this
 * transaction or only after it has committed.
 * <p>
 * Read side: the {@link HouseholdPlanOverview} read model is loaded through its
 * {@link HouseholdPlanOverviewQueryHandler}, bypassing the aggregate and its repository.
 * <p>
 * Contains no business rules of its own. The only check it makes, that the user has no plan
 * for the year yet, is a process step before creating one: it spans several aggregates
 * and gives a clear error early, while the unique constraint behind
 * {@link HouseholdPlanRepository#add} remains the actual safeguard against races.
 * Concurrent changes to the same plan are serialized by {@link HouseholdPlanRepository#findById}.
 */
@Service
@Transactional
public class HouseholdPlanApplicationService {

    private final HouseholdPlanRepository repository;
    private final HouseholdPlanOverviewQueryHandler overviewQueryHandler;
    private final ApplicationEventPublisher eventPublisher;

    public HouseholdPlanApplicationService(HouseholdPlanRepository repository,
                                           HouseholdPlanOverviewQueryHandler overviewQueryHandler,
                                           ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.overviewQueryHandler = Objects.requireNonNull(overviewQueryHandler);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    public CreatedHouseholdPlan createPlan(CreateHouseholdPlanCommand command) {
        if (repository.existsFor(command.owner(), command.year())) {
            throw new HouseholdPlanAlreadyExistsException(command.year());
        }
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), command.owner(), command.year());
        repository.add(plan);
        publishEvents(plan);
        return new CreatedHouseholdPlan(plan.id(), overviewOf(plan, command.owner()));
    }

    public AddedBudgetItem addIncome(AddIncomeCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        BudgetItemId itemId = plan.addIncome(command.description(), command.amount(), command.subcategory(),
                command.note());
        saveAndPublishEvents(plan);
        return new AddedBudgetItem(itemId, overviewOf(plan, command.owner()));
    }

    public AddedBudgetItem addExpense(AddExpenseCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        BudgetItemId itemId = plan.addExpense(command.description(), command.amount(), command.category(),
                command.subcategory(), command.note());
        saveAndPublishEvents(plan);
        return new AddedBudgetItem(itemId, overviewOf(plan, command.owner()));
    }

    public HouseholdPlanOverview changeItem(ChangeBudgetItemCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.changeItem(command.itemId(), command.description(), command.amount(), command.category(),
                command.subcategory(), command.note());
        saveAndPublishEvents(plan);
        return overviewOf(plan, command.owner());
    }

    public HouseholdPlanOverview removeItem(RemoveBudgetItemCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.removeItem(command.itemId());
        saveAndPublishEvents(plan);
        return overviewOf(plan, command.owner());
    }

    public HouseholdPlanOverview reorderItems(ReorderBudgetItemsCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.reorderItems(command.order());
        saveAndPublishEvents(plan);
        return overviewOf(plan, command.owner());
    }

    public HouseholdPlanOverview reorderSubcategories(ReorderSubcategoriesCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.reorderSubcategories(command.section(), command.order());
        saveAndPublishEvents(plan);
        return overviewOf(plan, command.owner());
    }

    /**
     * @throws HouseholdPlanNotFoundException if the owner has no plan for the year
     */
    @Transactional(readOnly = true)
    public HouseholdPlanOverview getOverview(OwnerId owner, Year year) {
        return overviewQueryHandler.findByOwnerAndYear(owner, year)
                .orElseThrow(() -> new HouseholdPlanNotFoundException(year));
    }

    /** All plans of the owner with their totals and changes from the previous year; only reachable years (HP-1, JU-1). */
    @Transactional(readOnly = true)
    public YearlyOverview getYearlyOverview(OwnerId owner) {
        return overviewQueryHandler.findYearlyOverviewByOwner(owner,
                Year.of(HouseholdPlan.FIRST_YEAR), Year.of(HouseholdPlan.LAST_YEAR));
    }

    private HouseholdPlan load(HouseholdPlanId id, OwnerId owner) {
        return repository.findById(id)
                .filter(plan -> plan.isOwnedBy(owner))
                .orElseThrow(() -> new HouseholdPlanNotFoundException(id));
    }

    /** The overview of a plan just saved; it must exist, because this transaction wrote it. */
    private HouseholdPlanOverview overviewOf(HouseholdPlan plan, OwnerId owner) {
        return overviewQueryHandler.findByOwnerAndYear(owner, plan.year())
                .orElseThrow(() -> new IllegalStateException("No overview for saved plan " + plan.id()));
    }

    private void saveAndPublishEvents(HouseholdPlan plan) {
        repository.save(plan);
        publishEvents(plan);
    }

    private void publishEvents(HouseholdPlan plan) {
        plan.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
