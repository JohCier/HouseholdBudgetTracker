package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Change the description, amount, subcategory, note and, for an expense, the category of a position of a
 * household plan of the given owner. Whether a category fits the position is decided by
 * the plan, as only it knows whether the position is an income or an expense.
 *
 * @param category    the new category of an expense; {@code null} for an income
 * @param subcategory the new subcategory; {@code null} to remove it
 * @param note        the new note; {@code null} to remove it
 */
@Command
public record ChangeBudgetItemCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemId itemId,
                                      BudgetItemDescription description, Money amount, ExpenseCategory category,
                                      BudgetItemSubcategory subcategory, BudgetItemNote note) {

    public ChangeBudgetItemCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
