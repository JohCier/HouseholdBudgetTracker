package de.jcss.hbt.application.port.out;

import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;

import java.time.Year;
import java.util.List;

/**
 * Outbound (driven) port for persisting {@link BudgetItem} aggregates.
 */
public interface BudgetItemRepository {

    void add(BudgetItem item);

    /** @return {@code false} if no item with this id exists */
    boolean remove(BudgetItemId id);

    /** Items booked in the given year, newest booking date first. */
    List<BudgetItem> findByYear(Year year);
}
