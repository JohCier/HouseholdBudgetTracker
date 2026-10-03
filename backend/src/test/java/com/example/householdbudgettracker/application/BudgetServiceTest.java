package com.example.householdbudgettracker.application;

import com.example.householdbudgettracker.application.port.in.AddExpenseUseCase.AddExpenseCommand;
import com.example.householdbudgettracker.application.port.in.AddIncomeUseCase.AddIncomeCommand;
import com.example.householdbudgettracker.application.port.out.BudgetRepository;
import com.example.householdbudgettracker.domain.Budget;
import com.example.householdbudgettracker.domain.BudgetItemId;
import com.example.householdbudgettracker.domain.ExpenseCategory;
import com.example.householdbudgettracker.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link BudgetService} correctly orchestrates the port-out repository
 * around the aggregate's domain behaviour, using a real {@link Budget} aggregate and
 * a mocked repository to isolate the application layer under test.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    private BudgetService budgetService;
    private Budget budget;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository);
        budget = Budget.createEmpty();
        when(budgetRepository.load()).thenReturn(budget);
    }

    @Test
    void addIncomeAddsItemAndSavesBudget() {
        BudgetItemId id = budgetService.addIncome(new AddIncomeCommand("Gehalt", new BigDecimal("2000.00")));

        assertThat(budget.items()).extracting(item -> item.id()).containsExactly(id);
        verify(budgetRepository).save(budget);
    }

    @Test
    void addExpenseAddsItemAndSavesBudget() {
        BudgetItemId id = budgetService.addExpense(
                new AddExpenseCommand("Miete", new BigDecimal("800.00"), ExpenseCategory.LEBENSUNTERHALT));

        assertThat(budget.items()).extracting(item -> item.id()).containsExactly(id);
        verify(budgetRepository).save(budget);
    }

    @Test
    void removeItemDelegatesToAggregateAndSaves() {
        BudgetItemId id = budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        budgetService.removeItem(id);

        assertThat(budget.items()).isEmpty();
        verify(budgetRepository).save(budget);
    }

    @Test
    void listItemsReturnsCurrentBudgetItems() {
        budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        assertThat(budgetService.listItems()).hasSize(1);
    }

    @Test
    void getOverviewDelegatesToAggregate() {
        budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        assertThat(budgetService.getOverview().totalIncome().value()).isEqualByComparingTo("2000.00");
    }
}
