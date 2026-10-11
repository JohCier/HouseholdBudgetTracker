package de.jcss.hbt.application.householdplan.command;

import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainCommand;

public record RemoveBudgetItemCommand(HouseholdPlanId planId, BudgetItemId itemId) implements DomainCommand {
}
