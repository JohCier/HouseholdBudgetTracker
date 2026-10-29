package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Delete a deleted household plan of the given owner for good, with all its items (GP-16 to GP-18). */
@Command
public record PurgeHouseholdPlanCommand(OwnerId owner, HouseholdPlanId planId) {

    public PurgeHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
    }
}
