package de.jcss.hbt.domain.onetimeitem.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Part of the {@link OneTimeItemOverview} read model: one one-time item as shown in its section (EP-8).
 *
 * @param note {@code null} if the item has no note
 */
@QueryModel
public record OneTimeOverviewItem(OneTimeItemId id, ItemType type, BudgetItemDescription description, Money amount,
                                  LocalDate date, BudgetItemNote note) {

    public OneTimeOverviewItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(date, "date must not be null");
    }
}
