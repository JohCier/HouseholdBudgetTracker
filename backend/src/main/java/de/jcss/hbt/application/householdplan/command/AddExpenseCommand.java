package de.jcss.hbt.application.householdplan.command;

import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.math.BigDecimal;

public record AddExpenseCommand(HouseholdPlanId planId, String description, BigDecimal amount,
                                ExpenseCategory category) implements DomainCommand {
}
