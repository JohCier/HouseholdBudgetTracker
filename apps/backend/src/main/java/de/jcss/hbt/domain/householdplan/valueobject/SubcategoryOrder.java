package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The order of the subcategory groups within one {@link ItemSection} as the user arranged it
 * (UK-14). Empty until the user moves a group for the first time. It may name groups that the
 * section no longer has and lack groups that came up after it was arranged; {@link #arrange}
 * resolves both (UK-16, UK-17).
 */
@ValueObject
public record SubcategoryOrder(List<BudgetItemSubcategory> subcategories) {

    public static final SubcategoryOrder NONE = new SubcategoryOrder(List.of());

    public SubcategoryOrder {
        subcategories = List.copyOf(subcategories);
        for (int i = 0; i < subcategories.size(); i++) {
            for (int j = i + 1; j < subcategories.size(); j++) {
                if (subcategories.get(i).isSameAs(subcategories.get(j))) {
                    throw new DomainValidationException(
                            "subcategory %s must not be listed twice".formatted(subcategories.get(i)));
                }
            }
        }
    }

    /**
     * Puts the given groups of a section in display order: first those this order names, as
     * arranged; then the others alphabetically. "Sonstiges" stays last as long as it was not
     * arranged elsewhere, so new groups come before it (UK-16, UK-17).
     *
     * @param groups the groups the section has, each once
     * @return the given groups, in display order
     */
    public List<BudgetItemSubcategory> arrange(Collection<BudgetItemSubcategory> groups) {
        List<BudgetItemSubcategory> arranged = new ArrayList<>();
        for (BudgetItemSubcategory subcategory : subcategories) {
            groups.stream().filter(subcategory::isSameAs).findFirst().ifPresent(arranged::add);
        }
        List<BudgetItemSubcategory> unarranged = groups.stream()
                .filter(group -> !names(group))
                .sorted(alphabetically())
                .toList();
        BudgetItemSubcategory other = unarranged.stream().filter(BudgetItemSubcategory::isOther).findFirst()
                .orElse(null);
        boolean otherLast = !arranged.isEmpty() && arranged.getLast().isOther();
        int insertAt = otherLast ? arranged.size() - 1 : arranged.size();
        arranged.addAll(insertAt, unarranged.stream().filter(group -> !group.isOther()).toList());
        if (other != null) {
            arranged.add(other);
        }
        return arranged;
    }

    /** Whether this order names the given group. */
    public boolean names(BudgetItemSubcategory group) {
        return subcategories.stream().anyMatch(group::isSameAs);
    }

    /** This order without the groups the section no longer has (UK-18). */
    public SubcategoryOrder retainingOnly(Collection<BudgetItemSubcategory> groups) {
        return new SubcategoryOrder(subcategories.stream()
                .filter(subcategory -> groups.stream().anyMatch(subcategory::isSameAs))
                .toList());
    }

    public boolean isEmpty() {
        return subcategories.isEmpty();
    }

    /**
     * German alphabetical order ignoring case, e.g. "auto" before "Bildung" before "Übungsleiter".
     * A new comparator per call, as a {@link Collator} must not be shared between threads.
     */
    private static Comparator<BudgetItemSubcategory> alphabetically() {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        collator.setStrength(Collator.SECONDARY);
        return Comparator.comparing(BudgetItemSubcategory::value, collator);
    }
}
