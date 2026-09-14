package com.example.householdbudgettracker.application.port.in;

import com.example.householdbudgettracker.domain.BudgetItemId;

import java.math.BigDecimal;

/**
 * Inbound (driving) port for adding an income item to the budget.
 */
public interface AddIncomeUseCase {

    BudgetItemId addIncome(AddIncomeCommand command);

    record AddIncomeCommand(String description, BigDecimal amount) {
    }
}
