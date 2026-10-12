package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.util.Objects;

/** Plan an income of the given amount in a household plan. */
public record AddIncomeCommand(HouseholdPlanId planId, BudgetItemDescription description, Money amount)
        implements DomainCommand {

    public AddIncomeCommand {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
