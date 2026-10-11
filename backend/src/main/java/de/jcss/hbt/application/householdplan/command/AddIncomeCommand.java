package de.jcss.hbt.application.householdplan.command;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.math.BigDecimal;

public record AddIncomeCommand(HouseholdPlanId planId, String description, BigDecimal amount)
        implements DomainCommand {
}
