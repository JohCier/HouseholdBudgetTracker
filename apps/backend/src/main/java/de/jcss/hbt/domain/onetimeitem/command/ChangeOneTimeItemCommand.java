package de.jcss.hbt.domain.onetimeitem.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Replace type, description, amount, date and note of a one-time item of the given owner (EP-7).
 *
 * @param note {@code null} if the item is to have no note
 */
@Command
public record ChangeOneTimeItemCommand(OwnerId owner, OneTimeItemId itemId, ItemType type,
                                       BudgetItemDescription description, Money amount, LocalDate date,
                                       BudgetItemNote note) {

    public ChangeOneTimeItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(date, "date must not be null");
    }
}
