package de.jcss.hbt.domain.householdplan;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Whether the amount a user entered for a {@link BudgetItem} is a monthly or a yearly amount (MJ-1).
 */
@ValueObject
public enum AmountPeriod {
    MONTHLY,
    YEARLY
}
