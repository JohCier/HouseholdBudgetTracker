package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/**
 * Take over a household plan of a user as template for another year (PU-1, PU-2).
 *
 * @param overwrite whether a plan the user already has for the target year may be overwritten (PU-9)
 */
@Command
public record CopyHouseholdPlanCommand(OwnerId owner, HouseholdPlanId sourcePlanId, Year targetYear,
                                       boolean overwrite) {

    public CopyHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(sourcePlanId, "sourcePlanId must not be null");
        Objects.requireNonNull(targetYear, "targetYear must not be null");
    }
}
