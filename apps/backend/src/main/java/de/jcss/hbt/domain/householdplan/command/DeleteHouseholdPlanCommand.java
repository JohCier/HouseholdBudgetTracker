package de.jcss.hbt.domain.householdplan.command;

import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.util.Objects;

/** Delete a household plan of the given owner with all its items (PL-1, PL-2). */
@Command
public record DeleteHouseholdPlanCommand(OwnerId owner, HouseholdPlanId planId) {

    public DeleteHouseholdPlanCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
    }
}
