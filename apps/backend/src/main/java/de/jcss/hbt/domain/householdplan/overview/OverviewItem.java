package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import org.jmolecules.architecture.cqrs.QueryModel;

/**
 * Part of the {@link HouseholdPlanOverview} read model: a single position of the plan, as
 * shown in the item list.
 *
 * @param amount      as the user entered it, monthly or yearly (MJ-2)
 * @param category    only set for expenses, otherwise {@code null}
 * @param subcategory {@code null} if the item has no subcategory
 * @param note        {@code null} if the item has no note
 * @param marks       whether it is important and its saving potential, if any (PM-1, PM-2)
 */
@QueryModel
public record OverviewItem(
        BudgetItemId id,
        BudgetItemDescription description,
        ItemAmount amount,
        ItemType type,
        ExpenseCategory category,
        BudgetItemSubcategory subcategory,
        BudgetItemNote note,
        ItemMarks marks
) {

    /** An item without marks. */
    public OverviewItem(BudgetItemId id, BudgetItemDescription description, ItemAmount amount, ItemType type,
                        ExpenseCategory category, BudgetItemSubcategory subcategory, BudgetItemNote note) {
        this(id, description, amount, type, category, subcategory, note, ItemMarks.NONE);
    }

    /** The section the item is listed in (UK-9, UK-10). */
    public ItemSection section() {
        return new ItemSection(type, category);
    }

    /** The subcategory group the item is listed in (UK-1). */
    public BudgetItemSubcategory group() {
        return BudgetItemSubcategory.groupOf(subcategory);
    }
}
