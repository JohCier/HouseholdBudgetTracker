package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a single income or expense position of a {@link HouseholdPlan}. It
 * has no date of its own: it belongs to the year of the plan it lives in. Items are
 * immutable and only added to, changed in or removed from a plan through the plan itself:
 * new items are created by the package-private {@link #income} / {@link #expense} factories,
 * changed ones by {@link #changedTo}, which only the plan calls; stored items are rebuilt by
 * repositories via {@link #reconstitute}. All of them enforce the domain rules:
 * <ul>
 *     <li>description is required (its own rules are enforced by {@link BudgetItemDescription})</li>
 *     <li>amount must be positive; it is a monthly or a yearly amount (MJ-1)</li>
 *     <li>an expense item requires a category; an income item never has one</li>
 *     <li>the subcategory is optional for both (its own rules are enforced by {@link BudgetItemSubcategory})</li>
 *     <li>the note is optional for both (its own rules are enforced by {@link BudgetItemNote})</li>
 *     <li>both may be marked as important; only an expense may have a {@link SavingPotential}, and at
 *     most its monthly amount (PM-1, PM-2, PM-2a)</li>
 * </ul>
 * Changing the values of an item keeps its marks; a saving potential above the new monthly amount is
 * lowered to it (PM-5).
 * Identity (equals/hashCode) is based solely on the item id, as is appropriate for
 * a DDD entity.
 */
@Entity
public final class BudgetItem {

    @Identity
    private final BudgetItemId id;
    private final BudgetItemDescription description;
    private final ItemAmount amount;
    private final ItemType type;
    private final ExpenseCategory category;
    private final BudgetItemSubcategory subcategory;
    private final BudgetItemNote note;
    private final boolean important;
    private final SavingPotential savingPotential;

    private BudgetItem(BudgetItemId id, BudgetItemDescription description, ItemAmount amount, ItemType type,
                       ExpenseCategory category, BudgetItemSubcategory subcategory, BudgetItemNote note,
                       boolean important, SavingPotential savingPotential) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = requireDescription(description);
        this.amount = requirePositive(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = category;
        this.subcategory = subcategory;
        this.note = note;
        this.important = important;
        this.savingPotential = requireFitting(savingPotential, type, this.amount);
    }

    /**
     * @param subcategory {@code null} if the item has no subcategory
     * @param note        {@code null} if the item has no note
     */
    static BudgetItem income(BudgetItemId id, BudgetItemDescription description, ItemAmount amount,
                             BudgetItemSubcategory subcategory, BudgetItemNote note) {
        return new BudgetItem(id, description, amount, ItemType.INCOME, null, subcategory, note, false, null);
    }

    /**
     * @param subcategory {@code null} if the item has no subcategory
     * @param note        {@code null} if the item has no note
     */
    static BudgetItem expense(BudgetItemId id, BudgetItemDescription description, ItemAmount amount,
                              ExpenseCategory category, BudgetItemSubcategory subcategory, BudgetItemNote note) {
        if (category == null) {
            throw new DomainValidationException("category is required for expense items");
        }
        return new BudgetItem(id, description, amount, ItemType.EXPENSE, category, subcategory, note, false, null);
    }

    /**
     * Rebuilds a stored item, e.g. in a repository; only meant for persistence.
     *
     * @param category    the category of an expense; {@code null} for an income
     * @param subcategory {@code null} if the item has no subcategory
     * @param note            {@code null} if the item has no note
     * @param savingPotential {@code null} if the item has none
     */
    public static BudgetItem reconstitute(BudgetItemId id, BudgetItemDescription description, ItemAmount amount,
                                          ItemType type, ExpenseCategory category,
                                          BudgetItemSubcategory subcategory, BudgetItemNote note,
                                          boolean important, SavingPotential savingPotential) {
        BudgetItem item = switch (Objects.requireNonNull(type, "type must not be null")) {
            case INCOME -> {
                if (category != null) {
                    throw new DomainValidationException("income items must not have a category");
                }
                yield income(id, description, amount, subcategory, note);
            }
            case EXPENSE -> expense(id, description, amount, category, subcategory, note);
        };
        return item.withImportant(important).withSavingPotential(savingPotential);
    }

    /** Rebuilds a stored item without marks, see {@link #reconstitute(BudgetItemId, BudgetItemDescription, ItemAmount, ItemType, ExpenseCategory, BudgetItemSubcategory, BudgetItemNote, boolean, SavingPotential)}. */
    public static BudgetItem reconstitute(BudgetItemId id, BudgetItemDescription description, ItemAmount amount,
                                          ItemType type, ExpenseCategory category,
                                          BudgetItemSubcategory subcategory, BudgetItemNote note) {
        return reconstitute(id, description, amount, type, category, subcategory, note, false, null);
    }

    /**
     * Returns a copy of this item with the same id, type and marks but the given values; a saving
     * potential above the new monthly amount is lowered to it (PM-5). The type
     * itself cannot be changed: an income must not get a category, an expense needs one.
     *
     * @param category    the new category of an expense; {@code null} for an income
     * @param subcategory the new subcategory; {@code null} to remove it
     * @param note        the new note; {@code null} to remove it
     */
    BudgetItem changedTo(BudgetItemDescription description, ItemAmount amount, ExpenseCategory category,
                         BudgetItemSubcategory subcategory, BudgetItemNote note) {
        return reconstitute(id, description, amount, type, category, subcategory, note, important,
                savingPotential == null ? null : savingPotential.cappedTo(amount));
    }

    /** Returns a copy of this item with a new id and the same values and marks, for another plan (PU-3, PM-6). */
    BudgetItem copy() {
        return reconstitute(BudgetItemId.generate(), description, amount, type, category, subcategory, note, important,
                savingPotential);
    }

    /** Returns a copy of this item that is marked as important or not (PM-1). */
    BudgetItem withImportant(boolean important) {
        return new BudgetItem(id, description, amount, type, category, subcategory, note, important, savingPotential);
    }

    /**
     * Returns a copy of this item with the given saving potential (PM-2).
     *
     * @param savingPotential {@code null} to remove it
     * @throws DomainValidationException if an income gets a saving potential, or an expense one above its monthly amount
     */
    BudgetItem withSavingPotential(SavingPotential savingPotential) {
        return new BudgetItem(id, description, amount, type, category, subcategory, note, important, savingPotential);
    }

    /** Whether the other item has the same description, amount, type, category, subcategory, note and marks as this one. */
    boolean hasSameValuesAs(BudgetItem other) {
        return description.equals(other.description) && amount.equals(other.amount) && type == other.type
                && Objects.equals(category, other.category) && Objects.equals(subcategory, other.subcategory)
                && Objects.equals(note, other.note) && important == other.important
                && Objects.equals(savingPotential, other.savingPotential);
    }

    /** The section the item is listed in: the incomes or the expenses of its category (UK-9, UK-10). */
    public ItemSection section() {
        return new ItemSection(type, category);
    }

    /** The subcategory group the item is listed in, {@link BudgetItemSubcategory#OTHER} if it has none (UK-1). */
    public BudgetItemSubcategory group() {
        return BudgetItemSubcategory.groupOf(subcategory);
    }

    /** Whether the other item is listed in the same section and subcategory group as this one (UK-11). */
    boolean isInSameGroupAs(BudgetItem other) {
        return section().equals(other.section()) && group().isSameAs(other.group());
    }

    private static BudgetItemDescription requireDescription(BudgetItemDescription description) {
        if (description == null) {
            throw new DomainValidationException("description is required");
        }
        return description;
    }

    private static SavingPotential requireFitting(SavingPotential savingPotential, ItemType type, ItemAmount amount) {
        if (savingPotential != null && type == ItemType.INCOME) {
            throw new DomainValidationException("income items must not have a saving potential");
        }
        if (savingPotential != null && savingPotential.exceeds(amount)) {
            throw new DomainValidationException(
                    "saving potential must not exceed the monthly amount of the expense, %s"
                            .formatted(amount.monthly().value().toPlainString()));
        }
        return savingPotential;
    }

    private static ItemAmount requirePositive(ItemAmount amount) {
        if (amount == null || !amount.isPositive()) {
            throw new DomainValidationException("amount must be positive");
        }
        return amount;
    }

    boolean hasId(BudgetItemId itemId) {
        return id.equals(itemId);
    }

    /** Returns the given summary with this item's yearly amount (MJ-8) added as income or as expense of its category. */
    public BudgetSummary addTo(BudgetSummary summary) {
        return switch (type) {
            case INCOME -> summary.withIncome(amount.annual());
            case EXPENSE -> summary.withExpense(category, amount.annual());
        };
    }

    public BudgetItemId id() {
        return id;
    }

    public BudgetItemDescription description() {
        return description;
    }

    public ItemAmount amount() {
        return amount;
    }

    public ItemType type() {
        return type;
    }

    public Optional<ExpenseCategory> category() {
        return Optional.ofNullable(category);
    }

    public Optional<BudgetItemSubcategory> subcategory() {
        return Optional.ofNullable(subcategory);
    }

    public Optional<BudgetItemNote> note() {
        return Optional.ofNullable(note);
    }

    public boolean isImportant() {
        return important;
    }

    public Optional<SavingPotential> savingPotential() {
        return Optional.ofNullable(savingPotential);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BudgetItem other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BudgetItem{id=%s, description='%s', amount=%s, type=%s, category=%s, subcategory=%s, important=%s, savingPotential=%s}"
                .formatted(id, description, amount, type, category, subcategory, important, savingPotential);
    }
}
