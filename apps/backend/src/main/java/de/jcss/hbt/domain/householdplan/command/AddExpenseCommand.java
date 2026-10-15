package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.util.Objects;

/** Plan an expense of the given amount in a category of a household plan of the given owner. */
public record AddExpenseCommand(OwnerId owner, HouseholdPlanId planId, BudgetItemDescription description,
                                Money amount, ExpenseCategory category) implements DomainCommand {

    public AddExpenseCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(category, "category must not be null");
    }
}
