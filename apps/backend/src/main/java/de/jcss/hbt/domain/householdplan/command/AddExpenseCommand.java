package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Plan an expense of the given amount in a category of a household plan of the given owner.
 *
 * @param subcategory {@code null} if the expense has no subcategory
 * @param note        {@code null} if the expense has no note
 * @param marks       how the expense is marked; only set when an item is restored (PM-6), otherwise {@link ItemMarks#NONE}
 */
@Command
public record AddExpenseCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemDescription description,
                                ItemAmount amount, ExpenseCategory category,
                                BudgetItemSubcategory subcategory, BudgetItemNote note, ItemMarks marks) {

    public AddExpenseCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(marks, "marks must not be null");
    }

    /** An expense without marks, as every new one is (PM-1, PM-2). */
    public AddExpenseCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemDescription description,
                             ItemAmount amount, ExpenseCategory category, BudgetItemSubcategory subcategory,
                             BudgetItemNote note) {
        this(owner, planId, description, amount, category, subcategory, note, ItemMarks.NONE);
    }
}
