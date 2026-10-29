package de.jcss.hbt.domain.networth.command;

import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.jmolecules.architecture.cqrs.Command;

import java.time.Year;
import java.util.Objects;

/**
 * Take over the assets and liabilities of a year of the user into another year (VS-15).
 *
 * @param overwrite whether a statement the user already has for the target year may be overwritten (VS-16)
 */
@Command
public record CopyNetWorthStatementCommand(OwnerId owner, Year sourceYear, Year targetYear, boolean overwrite) {

    public CopyNetWorthStatementCommand {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(sourceYear, "sourceYear must not be null");
        Objects.requireNonNull(targetYear, "targetYear must not be null");
    }
}
