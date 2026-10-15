package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.shared.DomainCommand;

import java.time.Year;
import java.util.Objects;

/** Start a new, empty household plan of a user for a year. */
public record CreateHouseholdPlanCommand(OwnerId owner, Year year) implements DomainCommand {

    public CreateHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
    }
}
