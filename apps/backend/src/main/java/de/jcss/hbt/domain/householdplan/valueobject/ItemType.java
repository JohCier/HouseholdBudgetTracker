package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.householdplan.BudgetItem;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Distinguishes whether a {@link BudgetItem} contributes to income or expenses.
 */
@ValueObject
public enum ItemType {
    INCOME,
    EXPENSE
}
