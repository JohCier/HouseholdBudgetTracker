package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.householdplan.AddedBudgetItem;
import de.jcss.hbt.application.householdplan.CopiedHouseholdPlan;
import de.jcss.hbt.application.householdplan.HouseholdPlanApplicationService;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.command.AddExpenseCommand;
import de.jcss.hbt.domain.householdplan.command.AddIncomeCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ChangeSavingPotentialCommand;
import de.jcss.hbt.domain.householdplan.command.CopyHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.CreateHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.DeleteHouseholdPlanCommand;
import de.jcss.hbt.domain.householdplan.command.MarkBudgetItemImportantCommand;
import de.jcss.hbt.domain.householdplan.command.RemoveBudgetItemCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderBudgetItemsCommand;
import de.jcss.hbt.domain.householdplan.command.ReorderSubcategoriesCommand;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import de.jcss.hbt.infrastructure.web.dto.AddExpenseRequest;
import de.jcss.hbt.infrastructure.web.dto.AddIncomeRequest;
import de.jcss.hbt.infrastructure.web.dto.AddedBudgetItemResponse;
import de.jcss.hbt.infrastructure.web.dto.ChangeBudgetItemRequest;
import de.jcss.hbt.infrastructure.web.dto.CopiedHouseholdPlanResponse;
import de.jcss.hbt.infrastructure.web.dto.CopyHouseholdPlanRequest;
import de.jcss.hbt.infrastructure.web.dto.CreateHouseholdPlanRequest;
import de.jcss.hbt.infrastructure.web.dto.CreatedHouseholdPlanResponse;
import de.jcss.hbt.infrastructure.web.dto.ExpenseCategoryDto;
import de.jcss.hbt.infrastructure.web.dto.HouseholdPlanOverviewResponse;
import de.jcss.hbt.infrastructure.web.dto.MarkImportantRequest;
import de.jcss.hbt.infrastructure.web.dto.ReorderBudgetItemsRequest;
import de.jcss.hbt.infrastructure.web.dto.ReorderSubcategoriesRequest;
import de.jcss.hbt.infrastructure.web.dto.SavingPotentialRequest;
import de.jcss.hbt.infrastructure.web.dto.YearlyOverviewResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

/**
 * Exposes household plans as a REST API. Contains no business logic: requests that change
 * something become commands for the {@link HouseholdPlanApplicationService}, the overview
 * of a plan is read through it as well. Every change answers with the overview of the plan
 * after the change (AU-1), so clients need no second request. Everything is done on behalf of the signed-in user,
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
    public CreatedHouseholdPlanResponse createPlan(@AuthenticationPrincipal Jwt user,
                                                   @Valid @RequestBody CreateHouseholdPlanRequest request) {
        return CreatedHouseholdPlanResponse.from(
                plans.createPlan(new CreateHouseholdPlanCommand(owner(user), Year.of(request.year()))));
    }

    /** The overview of the user's plan of a year, including its items; 404 if they have no plan for it. */
    @GetMapping
    public HouseholdPlanOverviewResponse overview(@AuthenticationPrincipal Jwt user,
                                                  @RequestParam @Min(HouseholdPlan.FIRST_YEAR) @Max(HouseholdPlan.LAST_YEAR) int year) {
        return HouseholdPlanOverviewResponse.from(plans.getOverview(owner(user), Year.of(year)));
    }

    /** All plans of the user, the newest year first, with their totals and changes from the previous year (JU-1 to JU-9). */
    @GetMapping("/years")
    public YearlyOverviewResponse yearlyOverview(@AuthenticationPrincipal Jwt user) {
        return YearlyOverviewResponse.from(plans.getYearlyOverview(owner(user)));
    }

    /**
     * Takes over a plan as template for another year (PU-1 to PU-9): 201 with a new plan, 200 if the
     * plan of the target year was overwritten, 409 if it exists and may not be overwritten.
     */
    @PostMapping("/{planId}/copies")
    public ResponseEntity<CopiedHouseholdPlanResponse> copyPlan(@AuthenticationPrincipal Jwt user,
                                                                @PathVariable UUID planId,
                                                                @Valid @RequestBody CopyHouseholdPlanRequest request) {
        CopiedHouseholdPlan copied = plans.copyPlan(new CopyHouseholdPlanCommand(owner(user),
                HouseholdPlanId.of(planId), Year.of(request.targetYear()), Boolean.TRUE.equals(request.overwrite())));
        return ResponseEntity.status(copied.overwritten() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(CopiedHouseholdPlanResponse.from(copied));
    }

    /** Deletes a plan with all its items (PL-1, PL-2): 204, or 404 if it is not a plan of the user. */
    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId) {
        plans.deletePlan(new DeleteHouseholdPlanCommand(owner(user), HouseholdPlanId.of(planId)));
    }

    @PostMapping("/{planId}/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public AddedBudgetItemResponse addIncome(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                             @Valid @RequestBody AddIncomeRequest request) {
        AddedBudgetItem added = plans.addIncome(new AddIncomeCommand(owner(user), HouseholdPlanId.of(planId),
                BudgetItemDescription.of(request.description()), new ItemAmount(Money.of(request.amount()), request.period().toDomain()),
                BudgetItemSubcategory.ofNullable(request.subcategory()), BudgetItemNote.ofNullable(request.note()),
                marks(request.important(), null)));
        return AddedBudgetItemResponse.from(added);
    }

    @PostMapping("/{planId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddedBudgetItemResponse addExpense(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                              @Valid @RequestBody AddExpenseRequest request) {
        AddedBudgetItem added = plans.addExpense(new AddExpenseCommand(owner(user), HouseholdPlanId.of(planId),
                BudgetItemDescription.of(request.description()), new ItemAmount(Money.of(request.amount()), request.period().toDomain()),
                request.category().toDomain(), BudgetItemSubcategory.ofNullable(request.subcategory()),
                BudgetItemNote.ofNullable(request.note()), marks(request.important(), request.savingPotential())));
        return AddedBudgetItemResponse.from(added);
    }

    /** Gives an item new values; an expense needs a category, an income must not have one. */
    @PutMapping("/{planId}/items/{itemId}")
    public HouseholdPlanOverviewResponse changeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                                    @PathVariable UUID itemId,
                                                    @Valid @RequestBody ChangeBudgetItemRequest request) {
        ExpenseCategoryDto category = request.category();
        return HouseholdPlanOverviewResponse.from(plans.changeItem(new ChangeBudgetItemCommand(owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId),
                BudgetItemDescription.of(request.description()), new ItemAmount(Money.of(request.amount()), request.period().toDomain()),
                category == null ? null : category.toDomain(), BudgetItemSubcategory.ofNullable(request.subcategory()),
                BudgetItemNote.ofNullable(request.note()))));
    }

    /** Marks an item as important or no longer (PM-1, PM-4a). */
    @PutMapping("/{planId}/items/{itemId}/important")
    public HouseholdPlanOverviewResponse markImportant(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                                       @PathVariable UUID itemId,
                                                       @Valid @RequestBody MarkImportantRequest request) {
        return HouseholdPlanOverviewResponse.from(plans.markItemImportant(new MarkBudgetItemImportantCommand(
                owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId), request.important())));
    }

    /** Sets or changes the monthly saving potential of an expense (PM-2, PM-2a, PM-4b); 400 for an income or a too large amount. */
    @PutMapping("/{planId}/items/{itemId}/saving-potential")
    public HouseholdPlanOverviewResponse changeSavingPotential(@AuthenticationPrincipal Jwt user,
                                                               @PathVariable UUID planId, @PathVariable UUID itemId,
                                                               @Valid @RequestBody SavingPotentialRequest request) {
        return HouseholdPlanOverviewResponse.from(plans.changeSavingPotential(new ChangeSavingPotentialCommand(
                owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId),
                SavingPotential.monthly(Money.of(request.monthlyAmount())))));
    }

    /** Removes the saving potential of an expense (PM-4b). */
    @DeleteMapping("/{planId}/items/{itemId}/saving-potential")
    public HouseholdPlanOverviewResponse removeSavingPotential(@AuthenticationPrincipal Jwt user,
                                                               @PathVariable UUID planId, @PathVariable UUID itemId) {
        return HouseholdPlanOverviewResponse.from(plans.changeSavingPotential(new ChangeSavingPotentialCommand(
                owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId), null)));
    }

    /** Puts the items of one subcategory group in a new order (UK-14). */
    @PutMapping("/{planId}/item-order")
    public HouseholdPlanOverviewResponse reorderItems(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                                      @Valid @RequestBody ReorderBudgetItemsRequest request) {
        return HouseholdPlanOverviewResponse.from(plans.reorderItems(new ReorderBudgetItemsCommand(owner(user),
                HouseholdPlanId.of(planId), request.itemIds().stream().map(BudgetItemId::of).toList())));
    }

    /** Puts the subcategory groups of the incomes or of the expenses of one category in a new order (UK-14). */
    @PutMapping("/{planId}/subcategory-order")
    public HouseholdPlanOverviewResponse reorderSubcategories(@AuthenticationPrincipal Jwt user,
                                                              @PathVariable UUID planId,
                                                              @Valid @RequestBody ReorderSubcategoriesRequest request) {
        ExpenseCategoryDto category = request.category();
        ItemSection section = new ItemSection(request.type().toDomain(), category == null ? null : category.toDomain());
        return HouseholdPlanOverviewResponse.from(plans.reorderSubcategories(new ReorderSubcategoriesCommand(
                owner(user), HouseholdPlanId.of(planId), section,
                request.subcategories().stream().map(BudgetItemSubcategory::of).toList())));
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    public HouseholdPlanOverviewResponse removeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID planId,
                                                    @PathVariable UUID itemId) {
        return HouseholdPlanOverviewResponse.from(plans.removeItem(
                new RemoveBudgetItemCommand(owner(user), HouseholdPlanId.of(planId), BudgetItemId.of(itemId))));
    }

    /** The marks of an item to restore (PM-6); absent values mean none. */
    private static ItemMarks marks(Boolean important, BigDecimal savingPotential) {
        return new ItemMarks(Boolean.TRUE.equals(important),
                savingPotential == null ? null : SavingPotential.monthly(Money.of(savingPotential)));
    }

    /** The signed-in user; the subject of their session token is their user id. */
    private static OwnerId owner(Jwt user) {
        return OwnerId.of(UUID.fromString(user.getSubject()));
    }
}
