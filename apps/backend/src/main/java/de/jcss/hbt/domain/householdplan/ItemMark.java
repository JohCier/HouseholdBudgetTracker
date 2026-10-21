package de.jcss.hbt.domain.householdplan;

import org.jmolecules.ddd.annotation.ValueObject;

/** The marks a user can set on a {@link BudgetItem} (PM-1, PM-2), as named in the audit log (PM-7). */
@ValueObject
public enum ItemMark {
    IMPORTANT,
    SAVING_POTENTIAL
}
