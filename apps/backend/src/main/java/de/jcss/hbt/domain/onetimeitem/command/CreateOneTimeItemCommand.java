package de.jcss.hbt.domain.onetimeitem.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Enter a one-time income or expense of the given owner on the given date (EP-1, EP-7).
 *
 * @param note {@code null} if the item has no note
 */
@Command
public record CreateOneTimeItemCommand(OwnerId owner, ItemType type, BudgetItemDescription description,
                                       Money amount, LocalDate date, BudgetItemNote note) {

    public CreateOneTimeItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(date, "date must not be null");
    }
}
