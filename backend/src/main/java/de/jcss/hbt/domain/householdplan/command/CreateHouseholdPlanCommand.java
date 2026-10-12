package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.shared.DomainCommand;

import java.time.Year;
import java.util.Objects;

/** Start a new, empty household plan for a year. */
public record CreateHouseholdPlanCommand(Year year) implements DomainCommand {

    public CreateHouseholdPlanCommand {
        Objects.requireNonNull(year, "year must not be null");
    }
}
