package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/** Start a new, empty household plan of a user for a year. */
@Command
public record CreateHouseholdPlanCommand(OwnerId owner, Year year) {

    public CreateHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(year, "year must not be null");
    }
}
