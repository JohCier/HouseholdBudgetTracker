package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubcategoryOrderTest {

    private static final BudgetItemSubcategory OTHER = BudgetItemSubcategory.OTHER;

    /** UK-16 */
    @Test
    void withoutArrangementGroupsAreAlphabeticalWithOtherLast() {
        assertThat(SubcategoryOrder.NONE.arrange(groups("Sonstiges", "miete", "Übungsleiter", "Auto", "Bildung")))
                .containsExactlyElementsOf(groups("Auto", "Bildung", "miete", "Übungsleiter", "Sonstiges"));
    }

    /** UK-14 */
    @Test
    void keepsTheArrangedOrder() {
        SubcategoryOrder order = order("Miete", "Sonstiges", "Auto");

        assertThat(order.arrange(groups("Auto", "Miete", "Sonstiges")))
                .containsExactlyElementsOf(groups("Miete", "Sonstiges", "Auto"));
    }

    /** UK-5 */
    @Test
    void returnsTheGroupsInTheirOwnSpelling() {
        assertThat(order("AUTO", "miete").arrange(groups("Miete", "Auto")))
                .containsExactlyElementsOf(groups("Auto", "Miete"));
    }

    /** UK-17 */
    @Test
    void newGroupsComeBeforeOtherAsLongAsItIsLast() {
        SubcategoryOrder order = order("Miete", "Auto", "Sonstiges");

        assertThat(order.arrange(groups("Auto", "Urlaub", "Miete", "Sonstiges", "Bildung")))
                .containsExactlyElementsOf(groups("Miete", "Auto", "Bildung", "Urlaub", "Sonstiges"));
    }

    /** UK-17 */
    @Test
    void newGroupsComeLastIfOtherWasMovedUp() {
        SubcategoryOrder order = order("Sonstiges", "Miete");

        assertThat(order.arrange(groups("Miete", "Urlaub", "Sonstiges")))
                .containsExactlyElementsOf(groups("Sonstiges", "Miete", "Urlaub"));
    }

    /** UK-17 */
    @Test
    void newOtherGroupComesLast() {
        assertThat(order("Miete", "Auto").arrange(groups("Auto", "Sonstiges", "Urlaub", "Miete")))
                .containsExactlyElementsOf(groups("Miete", "Auto", "Urlaub", "Sonstiges"));
    }

    /** UK-18 */
    @Test
    void skipsGroupsTheSectionNoLongerHas() {
        assertThat(order("Miete", "Auto").arrange(groups("Auto"))).containsExactlyElementsOf(groups("Auto"));
        assertThat(order("Miete", "Auto").retainingOnly(groups("auto"))).isEqualTo(order("Auto"));
    }

    @Test
    void rejectsAGroupNamedTwice() {
        assertThatThrownBy(() -> order("Auto", "auto"))
                .isInstanceOf(DomainValidationException.class);
    }

    private static SubcategoryOrder order(String... names) {
        return new SubcategoryOrder(groups(names));
    }

    private static List<BudgetItemSubcategory> groups(String... names) {
        return Arrays.stream(names).map(name -> name.equals("Sonstiges") ? OTHER : BudgetItemSubcategory.of(name))
                .toList();
    }
}
