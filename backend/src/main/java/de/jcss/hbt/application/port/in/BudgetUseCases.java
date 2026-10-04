package de.jcss.hbt.application.port.in;

import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.BudgetOverview;
import de.jcss.hbt.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Inbound (driving) port bundling the use cases of the household budget.
 */
public interface BudgetUseCases {

    BudgetItemId addIncome(AddIncomeCommand command);

    BudgetItemId addExpense(AddExpenseCommand command);

    void removeItem(BudgetItemId id);

    /** Items booked in the given month, newest first. */
    List<BudgetItem> listItems(YearMonth month);

    /** Totals and 50/30/20 category breakdown for the given month. */
    BudgetOverview getOverview(YearMonth month);

    record AddIncomeCommand(String description, BigDecimal amount, LocalDate bookingDate) {
    }

    record AddExpenseCommand(String description, BigDecimal amount, ExpenseCategory category, LocalDate bookingDate) {
    }
}
