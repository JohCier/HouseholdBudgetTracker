package de.jcss.hbt.application.householdplan.command;

import de.jcss.hbt.domain.shared.DomainCommand;

import java.time.Year;

public record CreateHouseholdPlanCommand(Year year) implements DomainCommand {
}
