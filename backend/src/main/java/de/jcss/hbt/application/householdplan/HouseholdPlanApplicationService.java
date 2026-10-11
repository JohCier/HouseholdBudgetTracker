package de.jcss.hbt.application.householdplan;

import de.jcss.hbt.application.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.application.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.application.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.application.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.application.shared.ApplicationService;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Objects;

/**
 * Entry point for all use cases of household plans.
 * <p>
 * Write side: each command runs in its own transaction:
 * load the {@link HouseholdPlan}, let it apply the change, save it, and publish the
 * domain events it recorded. Events are published through Spring's
 * {@link ApplicationEventPublisher}, so listeners can choose to run within this
 * transaction or only after it has committed.
 * <p>
 * Read side: the {@link HouseholdPlanOverview} read model is loaded through its
 * {@link HouseholdPlanOverviewQueryHandler}, bypassing the aggregate and its repository.
 * <p>
 * Contains no business rules of its own, apart from the cross-aggregate rule that there
 * is at most one plan per year.
 */
@ApplicationService
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

    public HouseholdPlanId createPlan(CreateHouseholdPlanCommand command) {
        if (repository.existsForYear(command.year())) {
            throw new HouseholdPlanAlreadyExistsException(command.year());
        }
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), command.year());
        saveAndPublishEvents(plan);
        return plan.id();
    }

    public BudgetItemId addIncome(AddIncomeCommand command) {
        HouseholdPlan plan = load(command.planId());
        BudgetItemId itemId = plan.addIncome(command.description(), toMoney(command.amount()));
        saveAndPublishEvents(plan);
        return itemId;
    }

    public BudgetItemId addExpense(AddExpenseCommand command) {
        HouseholdPlan plan = load(command.planId());
        BudgetItemId itemId = plan.addExpense(command.description(), toMoney(command.amount()), command.category());
        saveAndPublishEvents(plan);
        return itemId;
    }

    public void removeItem(RemoveBudgetItemCommand command) {
        HouseholdPlan plan = load(command.planId());
        plan.removeItem(command.itemId());
        saveAndPublishEvents(plan);
    }

    /**
     * @throws HouseholdPlanNotFoundException if there is no plan for the year
     */
    @Transactional(readOnly = true)
    public HouseholdPlanOverview getOverview(Year year) {
        return overviewQueryHandler.findByYear(year).orElseThrow(() -> new HouseholdPlanNotFoundException(year));
    }

    private HouseholdPlan load(HouseholdPlanId id) {
        return repository.findById(id).orElseThrow(() -> new HouseholdPlanNotFoundException(id));
    }

    private void saveAndPublishEvents(HouseholdPlan plan) {
        repository.save(plan);
        plan.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    private static Money toMoney(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }
}
