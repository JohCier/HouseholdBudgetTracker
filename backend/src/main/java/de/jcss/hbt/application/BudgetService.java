package de.jcss.hbt.application;

import de.jcss.hbt.application.port.in.BudgetUseCases;
import de.jcss.hbt.application.port.out.BudgetItemRepository;
import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.BudgetOverview;
import de.jcss.hbt.domain.Money;
import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the budget use cases, delegating all domain rules to {@link BudgetItem}
 * and {@link BudgetOverview}. Contains no business logic of its own.
 */
@Service
public class BudgetService implements BudgetUseCases {

    private final BudgetItemRepository repository;

    public BudgetService(BudgetItemRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public BudgetItemId addIncome(AddIncomeCommand command) {
        BudgetItem item = BudgetItem.income(BudgetItemId.generate(), command.description(), toMoney(command.amount()),
                command.bookingDate());
        repository.add(item);
        return item.id();
    }

    @Override
    public BudgetItemId addExpense(AddExpenseCommand command) {
        BudgetItem item = BudgetItem.expense(BudgetItemId.generate(), command.description(), toMoney(command.amount()),
                command.category(), command.bookingDate());
        repository.add(item);
        return item.id();
    }

    @Override
    public void removeItem(BudgetItemId id) {
        if (!repository.remove(id)) {
            throw new BudgetItemNotFoundException(id);
        }
    }

    @Override
    public List<BudgetItem> listItems(Year year) {
        return repository.findByYear(year);
    }

    @Override
    public BudgetOverview getOverview(Year year) {
        return BudgetOverview.of(year, repository.findByYear(year));
    }

    private static Money toMoney(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }
}
