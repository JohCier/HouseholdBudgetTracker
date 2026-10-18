package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.infrastructure.web.dto.AddExpenseRequest;
import de.jcss.hbt.infrastructure.web.dto.AddIncomeRequest;
import de.jcss.hbt.infrastructure.web.dto.BudgetItemIdResponse;
import de.jcss.hbt.infrastructure.web.dto.ChangeBudgetItemRequest;
import de.jcss.hbt.infrastructure.web.dto.CreateHouseholdPlanRequest;
import de.jcss.hbt.infrastructure.web.dto.HouseholdPlanIdResponse;
import de.jcss.hbt.infrastructure.web.dto.ExpenseCategoryDto;
import de.jcss.hbt.infrastructure.web.dto.HouseholdPlanOverviewResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * of a plan is read through it as well. Everything is done on behalf of the signed-in user,
 * the subject of the session token checked by the security configuration.
 */
@RestController
@RequestMapping(path = "/api/plans", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "household-plans")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class HouseholdPlanController {

    private final HouseholdPlanApplicationService plans;

    public HouseholdPlanController(HouseholdPlanApplicationService plans) {
        this.plans = plans;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdPlanIdResponse createPlan(@AuthenticationPrincipal Jwt user,
                                              @Valid @RequestBody CreateHouseholdPlanRequest request) {
        return HouseholdPlanIdResponse.from(
                plans.createPlan(new CreateHouseholdPlanCommand(owner(user), Year.of(request.year()))));
    }

    /** The overview of the user's plan of a year, including its items; 404 if they have no plan for it. */
    @GetMapping
    public HouseholdPlanOverviewResponse overview(@AuthenticationPrincipal Jwt user,
                                                  @RequestParam @Min(HouseholdPlan.FIRST_YEAR) @Max(HouseholdPlan.LAST_YEAR) int year) {
        return HouseholdPlanOverviewResponse.from(plans.getOverview(owner(user), Year.of(year)));
    }

    @PostMapping("/{planId}/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addIncome(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                          @Valid @RequestBody AddIncomeRequest request) {
        BudgetItemId id = plans.addIncome(new AddIncomeCommand(owner(user), HouseholdPlanId.of(planId),
                BudgetItemDescription.of(request.description()), Money.of(request.amount())));
        return BudgetItemIdResponse.from(id);
    }

    @PostMapping("/{planId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addExpense(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                           @Valid @RequestBody AddExpenseRequest request) {
        BudgetItemId id = plans.addExpense(new AddExpenseCommand(owner(user), HouseholdPlanId.of(planId),
                BudgetItemDescription.of(request.description()), Money.of(request.amount()),
                request.category().toDomain()));
        return BudgetItemIdResponse.from(id);
    }

    /** Gives an item new values; an expense needs a category, an income must not have one. */
    @PutMapping("/{planId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId, @PathVariable UUID itemId,
                           @Valid @RequestBody ChangeBudgetItemRequest request) {
        ExpenseCategoryDto category = request.category();
        plans.changeItem(new ChangeBudgetItemCommand(owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId),
                BudgetItemDescription.of(request.description()), Money.of(request.amount()),
                category == null ? null : category.toDomain()));
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId, @PathVariable UUID itemId) {
        plans.removeItem(new RemoveBudgetItemCommand(owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId)));
    }

    /** The signed-in user; the subject of their session token is their user id. */
    private static OwnerId owner(Jwt user) {
        return OwnerId.of(UUID.fromString(user.getSubject()));
    }
}
