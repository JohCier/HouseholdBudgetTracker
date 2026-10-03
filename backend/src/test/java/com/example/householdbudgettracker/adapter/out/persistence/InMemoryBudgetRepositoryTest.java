package com.example.householdbudgettracker.adapter.out.persistence;

import com.example.householdbudgettracker.domain.Budget;
import com.example.householdbudgettracker.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBudgetRepositoryTest {

    private final InMemoryBudgetRepository repository = new InMemoryBudgetRepository();

    @Test
    void startsWithAnEmptyBudget() {
        assertThat(repository.load().items()).isEmpty();
    }

    @Test
    void savedBudgetIsReturnedByLoad() {
        Budget budget = repository.load();
        budget.addIncome("Gehalt", Money.of(new BigDecimal("2000.00")));

        repository.save(budget);

        assertThat(repository.load().items()).hasSize(1);
    }
}
