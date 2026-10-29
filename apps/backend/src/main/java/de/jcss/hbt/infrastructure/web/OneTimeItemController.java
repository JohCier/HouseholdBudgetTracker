package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.onetimeitem.OneTimeItemApplicationService;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.command.ChangeOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.CreateOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.DeleteOneTimeItemCommand;
import de.jcss.hbt.infrastructure.web.dto.CreatedOneTimeItemResponse;
import de.jcss.hbt.infrastructure.web.dto.OneTimeItemOverviewResponse;
import de.jcss.hbt.infrastructure.web.dto.OneTimeItemRequest;
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
 * Exposes the one-time items of the signed-in user as a REST API (EP-1 to EP-17). Contains no
 * business logic: changes become commands for the {@link OneTimeItemApplicationService}, the
 * items of a year are read through it as well. Changes answer without content; clients read the
 * items of the year they show again, since a changed date may move an item to another year (EP-7).
 */
@RestController
@RequestMapping(path = "/api/one-time-items", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "one-time-items")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
public class OneTimeItemController {

    private final OneTimeItemApplicationService items;

    public OneTimeItemController(OneTimeItemApplicationService items) {
        this.items = items;
    }

    /** The one-time items of the user in a year with their totals (EP-8, EP-9); 400 for a year out of range. */
    @GetMapping
    public OneTimeItemOverviewResponse oneTimeItemOverview(@AuthenticationPrincipal Jwt user,
                                                           @RequestParam @Min(HouseholdPlan.FIRST_YEAR) @Max(HouseholdPlan.LAST_YEAR) int year) {
        return OneTimeItemOverviewResponse.from(items.getOverview(owner(user), Year.of(year)));
    }

    /** Enters a one-time item, also for a year without a plan (EP-5): 201 with its id, 400 for invalid input. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedOneTimeItemResponse createOneTimeItem(@AuthenticationPrincipal Jwt user,
                                                        @Valid @RequestBody OneTimeItemRequest request) {
        OneTimeItemId id = items.createItem(new CreateOneTimeItemCommand(owner(user), request.type().toDomain(),
                BudgetItemDescription.of(request.description()), Money.of(request.amount()), request.date(),
                BudgetItemNote.ofNullable(request.note())));
        return new CreatedOneTimeItemResponse(id.value());
    }

    /** Changes a one-time item (EP-7): 204, or 404 if it is not an item of the user (EP-4). */
    @PutMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOneTimeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID itemId,
                                  @Valid @RequestBody OneTimeItemRequest request) {
        items.changeItem(new ChangeOneTimeItemCommand(owner(user), OneTimeItemId.of(itemId), request.type().toDomain(),
                BudgetItemDescription.of(request.description()), Money.of(request.amount()), request.date(),
                BudgetItemNote.ofNullable(request.note())));
    }

    /** Deletes a one-time item for good (EP-17): 204, or 404 if it is not an item of the user (EP-4). */
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOneTimeItem(@AuthenticationPrincipal Jwt user, @PathVariable UUID itemId) {
        items.deleteItem(new DeleteOneTimeItemCommand(owner(user), OneTimeItemId.of(itemId)));
    }

    private static OwnerId owner(Jwt user) {
        return OwnerId.of(UUID.fromString(user.getSubject()));
    }
}
