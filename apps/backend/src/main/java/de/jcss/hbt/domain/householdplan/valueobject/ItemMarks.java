package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.householdplan.BudgetItem;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * How the user marked a {@link BudgetItem} in the entries (PM-1 to PM-3): whether it is important,
 * and, for an expense, its {@link SavingPotential}. Both marks are independent of each other and
 * have no influence on the 50/30/20 evaluation (PM-8). Which items may have a saving potential and
 * how large it may be is enforced by {@link BudgetItem}.
 *
 * @param savingPotential {@code null} if the item has none
 */
@ValueObject
public record ItemMarks(boolean important, SavingPotential savingPotential) {

    /** The marks of a new item: neither important nor with a saving potential (PM-1, PM-2). */
    public static final ItemMarks NONE = new ItemMarks(false, null);

    public ItemMarks withImportant(boolean important) {
        return new ItemMarks(important, savingPotential);
    }

    /** @param savingPotential {@code null} to remove it */
    public ItemMarks withSavingPotential(SavingPotential savingPotential) {
        return new ItemMarks(important, savingPotential);
    }
}
