package com.example.householdbudgettracker.application.port.out;

import com.example.householdbudgettracker.domain.Budget;

/**
 * Outbound (driven) port for loading and persisting the single {@link Budget} aggregate.
 * Implemented by an adapter, e.g. an in-memory store.
 */
public interface BudgetRepository {

    Budget load();

    void save(Budget budget);
}
