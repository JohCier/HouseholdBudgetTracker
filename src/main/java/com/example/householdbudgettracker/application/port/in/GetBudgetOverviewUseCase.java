package com.example.householdbudgettracker.application.port.in;

import com.example.householdbudgettracker.domain.BudgetOverview;

/**
 * Inbound (driving) port for retrieving the aggregated budget overview, including the
 * 50/30/20 category breakdown.
 */
public interface GetBudgetOverviewUseCase {

    BudgetOverview getOverview();
}
