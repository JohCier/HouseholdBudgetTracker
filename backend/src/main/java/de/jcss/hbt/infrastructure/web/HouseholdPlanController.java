package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.infrastructure.web.dto.AddExpenseRequest;
import de.jcss.hbt.infrastructure.web.dto.AddIncomeRequest;
import de.jcss.hbt.infrastructure.web.dto.BudgetItemIdResponse;
import de.jcss.hbt.infrastructure.web.dto.CreateHouseholdPlanRequest;
import de.jcss.hbt.infrastructure.web.dto.HouseholdPlanIdResponse;
import de.jcss.hbt.infrastructure.web.dto.HouseholdPlanOverviewResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.UUID;

/**
 * Exposes household plans as a REST API. Contains no business logic: requests that change
 * something become commands for the {@link HouseholdPlanApplicationService}, the overview
 * of a plan is read through it as well.
 */
@RestController
@RequestMapping(path = "/api/plans", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "household-plans")
public class HouseholdPlanController {

    private final HouseholdPlanApplicationService plans;

    public HouseholdPlanController(HouseholdPlanApplicationService plans) {
        this.plans = plans;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdPlanIdResponse createPlan(@Valid @RequestBody CreateHouseholdPlanRequest request) {
        return HouseholdPlanIdResponse.from(plans.createPlan(new CreateHouseholdPlanCommand(Year.of(request.year()))));
    }

    /** The overview of the plan of a year, including its items; 404 if there is no plan for it. */
    @GetMapping
    public HouseholdPlanOverviewResponse overview(@RequestParam int year) {
        return HouseholdPlanOverviewResponse.from(plans.getOverview(Year.of(year)));
    }

    @PostMapping("/{planId}/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addIncome(@PathVariable UUID planId, @Valid @RequestBody AddIncomeRequest request) {
        BudgetItemId id = plans.addIncome(new AddIncomeCommand(
                HouseholdPlanId.of(planId), BudgetItemDescription.of(request.description()), Money.of(request.amount())));
        return BudgetItemIdResponse.from(id);
    }

    @PostMapping("/{planId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addExpense(@PathVariable UUID planId, @Valid @RequestBody AddExpenseRequest request) {
        BudgetItemId id = plans.addExpense(new AddExpenseCommand(HouseholdPlanId.of(planId),
                BudgetItemDescription.of(request.description()), Money.of(request.amount()),
                request.category().toDomain()));
        return BudgetItemIdResponse.from(id);
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID planId, @PathVariable UUID itemId) {
        plans.removeItem(new RemoveBudgetItemCommand(HouseholdPlanId.of(planId), BudgetItemId.of(itemId)));
    }
}
