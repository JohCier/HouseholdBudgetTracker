package de.jcss.hbt.adapter.in.web;

import de.jcss.hbt.adapter.in.web.dto.AddExpenseRequest;
import de.jcss.hbt.adapter.in.web.dto.AddIncomeRequest;
import de.jcss.hbt.adapter.in.web.dto.BudgetItemIdResponse;
import de.jcss.hbt.adapter.in.web.dto.BudgetItemResponse;
import de.jcss.hbt.adapter.in.web.dto.BudgetOverviewResponse;
import de.jcss.hbt.application.port.in.BudgetUseCases;
import de.jcss.hbt.application.port.in.BudgetUseCases.AddExpenseCommand;
import de.jcss.hbt.application.port.in.BudgetUseCases.AddIncomeCommand;
import de.jcss.hbt.domain.BudgetItemId;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;
import java.util.UUID;

/**
 * Primary (driving) web adapter exposing the budget use cases as a REST API.
 * Contains no business logic; it only translates HTTP requests/responses to and
 * from the application layer's port.
 */
@RestController
@RequestMapping(path = "/api/budget", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "budget")
public class BudgetController {

    private final BudgetUseCases budget;

    public BudgetController(BudgetUseCases budget) {
        this.budget = budget;
    }

    @PostMapping("/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addIncome(@Valid @RequestBody AddIncomeRequest request) {
        BudgetItemId id = budget.addIncome(
                new AddIncomeCommand(request.description(), request.amount(), request.bookingDate()));
        return BudgetItemIdResponse.from(id);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetItemIdResponse addExpense(@Valid @RequestBody AddExpenseRequest request) {
        BudgetItemId id = budget.addExpense(new AddExpenseCommand(
                request.description(), request.amount(), request.category().toDomain(), request.bookingDate()));
        return BudgetItemIdResponse.from(id);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID id) {
        budget.removeItem(BudgetItemId.of(id));
    }

    @GetMapping("/items")
    public List<BudgetItemResponse> listItems(
            @RequestParam @Parameter(schema = @Schema(type = "integer", format = "int32", example = "2026")) Year year) {
        return budget.listItems(year).stream().map(BudgetItemResponse::from).toList();
    }

    @GetMapping("/overview")
    public BudgetOverviewResponse overview(
            @RequestParam @Parameter(schema = @Schema(type = "integer", format = "int32", example = "2026")) Year year) {
        return BudgetOverviewResponse.from(budget.getOverview(year));
    }
}
