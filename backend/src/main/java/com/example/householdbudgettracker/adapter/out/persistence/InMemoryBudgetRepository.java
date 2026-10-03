package com.example.householdbudgettracker.adapter.out.persistence;

import com.example.householdbudgettracker.application.port.out.BudgetRepository;
import com.example.householdbudgettracker.domain.Budget;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Driven adapter holding the single {@link Budget} aggregate purely in memory.
 * As there is no user/household concept yet, this behaves as a process-wide singleton
 * store rather than a keyed collection.
 */
@Component
public class InMemoryBudgetRepository implements BudgetRepository {

    private volatile Budget budget = Budget.createEmpty();

    @Override
    public synchronized Budget load() {
        return budget;
    }

    @Override
    public synchronized void save(Budget budget) {
        this.budget = Objects.requireNonNull(budget);
    }
}
