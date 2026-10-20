package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Plan an expense of the given amount in a category of a household plan of the given owner.
 *
 * @param subcategory {@code null} if the expense has no subcategory
 * @param note        {@code null} if the expense has no note
 */
@Command
public record AddExpenseCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemDescription description,
                                ItemAmount amount, ExpenseCategory category,
                                BudgetItemSubcategory subcategory, BudgetItemNote note) {

    public AddExpenseCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(category, "category must not be null");
    }
}
