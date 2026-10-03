package com.example.householdbudgettracker.application.port.in;

import com.example.householdbudgettracker.domain.BudgetItemId;

/**
 * Inbound (driving) port for removing an existing budget item.
 */
public interface RemoveBudgetItemUseCase {

    void removeItem(BudgetItemId id);
}
