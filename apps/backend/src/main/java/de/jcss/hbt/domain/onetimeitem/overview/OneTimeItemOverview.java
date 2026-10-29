package de.jcss.hbt.domain.onetimeitem.overview;

import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * Read model of the one-time items of a user in one year (EP-8, EP-9): the items in the order they
 * are shown, by date and, on the same date, in the order they were entered, and their totals.
 * None of it counts towards the household plan of the year (EP-6).
 */
@QueryModel
public record OneTimeItemOverview(Year year, List<OneTimeOverviewItem> items) {

    public OneTimeItemOverview {
        Objects.requireNonNull(year, "year must not be null");
        items = List.copyOf(items);
    }

    /** The sum of all one-time incomes of the year (EP-9). */
    public Money totalIncome() {
        return totalOf(ItemType.INCOME);
    }

    /** The sum of all one-time expenses of the year (EP-9). */
    public Money totalExpense() {
        return totalOf(ItemType.EXPENSE);
    }

    /** One-time incomes minus one-time expenses; negative if the expenses are higher (EP-9). */
    public Money balance() {
        return totalIncome().subtract(totalExpense());
    }

    /** Whether the one-time expenses are higher than the one-time incomes, i.e. the balance is negative (EP-9). */
    public boolean expensesExceedIncome() {
        return totalExpense().compareTo(totalIncome()) > 0;
    }

    private Money totalOf(ItemType type) {
        return items.stream()
                .filter(item -> item.type() == type)
                .map(OneTimeOverviewItem::amount)
                .reduce(Money.ZERO, Money::add);
    }
}
