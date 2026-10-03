package com.example.householdbudgettracker.application.port.in;

import com.example.householdbudgettracker.domain.BudgetItem;

import java.util.List;

/**
 * Inbound (driving) port for listing all budget items currently recorded.
 */
public interface ListBudgetItemsUseCase {

    List<BudgetItem> listItems();
}
