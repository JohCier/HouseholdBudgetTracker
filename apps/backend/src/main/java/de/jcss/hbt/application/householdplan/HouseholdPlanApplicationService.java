package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeSavingPotentialCommand;
import de.jcss.hbt.domain.householdplan.command.CopyHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.DeleteHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.MarkBudgetItemImportantCommand;
import de.jcss.hbt.domain.householdplan.command.PurgeHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderBudgetItemsCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderSubcategoriesCommand;
import de.jcss.hbt.domain.householdplan.command.RestoreHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlans;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Objects;
import java.util.Optional;

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
 * Contains no business rules of its own. The only check it makes, that the user has no active
 * plan for the year yet, is a process step before creating or restoring one (when copying or
 * restoring a plan, before deciding whether to replace one): it spans several aggregates
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

    /**
     * Copies a plan of the user to the target year into a new plan (PU-8). If the user has a plan
     * for the target year, it is deleted first, but only if the command allows that (PU-9, GP-19).
     * The source is just read (PU-5). Replacing a plan changes two aggregates in one transaction,
     * so that it succeeds as a whole or not at all (PU-7).
     *
     * @throws HouseholdPlanNotFoundException      if the source is not an active plan of the user
     * @throws HouseholdPlanAlreadyExistsException if the user has a plan for the target year and it may not be overwritten
     */
    public CopiedHouseholdPlan copyPlan(CopyHouseholdPlanCommand command) {
        HouseholdPlan source = load(command.sourcePlanId(), command.owner());
        Optional<HouseholdPlan> existing = repository.findFor(command.owner(), command.targetYear());
        if (existing.isPresent() && !command.overwrite()) {
            throw new HouseholdPlanAlreadyExistsException(command.targetYear());
        }
        HouseholdPlan target = HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), command.targetYear(),
                existing.isPresent());
        existing.ifPresent(this::delete);
        repository.add(target);
        publishEvents(target);
        return new CopiedHouseholdPlan(target.id(), existing.isPresent(), overviewOf(target, command.owner()));
    }

    /**
     * Moves a plan of the user with all its items to the deleted plans (PL-1, GP-7); other plans stay as they
     * are (PL-3).
     *
     * @throws HouseholdPlanNotFoundException if it is not an active plan of the user
     */
    public void deletePlan(DeleteHouseholdPlanCommand command) {
        delete(load(command.planId(), command.owner()));
    }

    /**
     * Makes a deleted plan of the user active again (GP-11). If the user has an active plan for its
     * year, that one is deleted first, but only if the command allows that (GP-13, GP-14). Replacing
     * a plan changes two aggregates in one transaction, so that it succeeds as a whole or not at all
     * (GP-15).
     *
     * @throws HouseholdPlanNotFoundException      if it is not a deleted plan of the user
     * @throws HouseholdPlanAlreadyExistsException if the user has an active plan for the year and it may not be replaced
     */
    public void restorePlan(RestoreHouseholdPlanCommand command) {
        HouseholdPlan plan = loadDeleted(command.planId(), command.owner());
        Optional<HouseholdPlan> active = repository.findFor(command.owner(), plan.year());
        if (active.isPresent()) {
            if (!command.overwrite()) {
                throw new HouseholdPlanAlreadyExistsException(plan.year());
            }
            delete(active.get());
        }
        plan.restore();
        repository.restore(plan);
        publishEvents(plan);
    }

    /**
     * Deletes a deleted plan of the user for good, with all its items (GP-17).
     *
     * @throws HouseholdPlanNotFoundException if it is not a deleted plan of the user, e.g. an active one (GP-18)
     */
    public void purgePlan(PurgeHouseholdPlanCommand command) {
        HouseholdPlan plan = loadDeleted(command.planId(), command.owner());
        plan.purge();
        repository.remove(plan);
        publishEvents(plan);
    }

    private void delete(HouseholdPlan plan) {
        plan.delete();
        repository.markDeleted(plan);
        publishEvents(plan);
    }

    public AddedBudgetItem addIncome(AddIncomeCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        BudgetItemId itemId = plan.addIncome(command.description(), command.amount(), command.subcategory(),
                command.note(), command.important());
        saveAndPublishEvents(plan);
        return new AddedBudgetItem(itemId, overviewOf(plan, command.owner()));
    }

    public AddedBudgetItem addExpense(AddExpenseCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        BudgetItemId itemId = plan.addExpense(command.description(), command.amount(), command.category(),
                command.subcategory(), command.note(), command.important(), command.savingPotential());
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

    public HouseholdPlanOverview markItemImportant(MarkBudgetItemImportantCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.markImportant(command.itemId(), command.important());
        saveAndPublishEvents(plan);
        return overviewOf(plan, command.owner());
    }

    public HouseholdPlanOverview changeSavingPotential(ChangeSavingPotentialCommand command) {
        HouseholdPlan plan = load(command.planId(), command.owner());
        plan.changeSavingPotential(command.itemId(), command.savingPotential());
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

    /**
     * All years of the owner with an active plan or one-time items: the totals of the plan with their changes
     * from the previous year and the totals of the one-time items, also of years without a plan, and how many
     * deleted plans the owner has; only reachable years (HP-1, JU-1, EP-12, EP-13, GP-21).
     */
    @Transactional(readOnly = true)
    public YearlyOverview getYearlyOverview(OwnerId owner) {
        return overviewQueryHandler.findYearlyOverviewByOwner(owner,
                Year.of(HouseholdPlan.FIRST_YEAR), Year.of(HouseholdPlan.LAST_YEAR));
    }

    /** The deleted plans of the owner with their totals; only reachable years (HP-1, GP-22). */
    @Transactional(readOnly = true)
    public DeletedPlans getDeletedPlans(OwnerId owner) {
        return overviewQueryHandler.findDeletedPlansByOwner(owner,
                Year.of(HouseholdPlan.FIRST_YEAR), Year.of(HouseholdPlan.LAST_YEAR));
    }

    private HouseholdPlan load(HouseholdPlanId id, OwnerId owner) {
        return repository.findById(id)
                .filter(plan -> plan.isOwnedBy(owner))
                .orElseThrow(() -> new HouseholdPlanNotFoundException(id));
    }

    private HouseholdPlan loadDeleted(HouseholdPlanId id, OwnerId owner) {
        return repository.findDeletedById(id)
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
