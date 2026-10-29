package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/**
 * Make a deleted household plan of the given owner active again (GP-11).
 *
 * @param overwrite whether an active plan the owner has for the same year may be replaced, i.e. deleted (GP-14)
 */
@Command
public record RestoreHouseholdPlanCommand(OwnerId owner, HouseholdPlanId planId, boolean overwrite) {

    public RestoreHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
    }
}
