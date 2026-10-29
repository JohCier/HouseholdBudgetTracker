package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemNoteTest {

    /** PN-2, PN-3 */
    @Test
    void keepsTheTextWithLineBreaksButWithoutSurroundingWhitespace() {
        assertThat(new BudgetItemNote("  Warmmiete\ninkl. Nebenkosten \n").value()).isEqualTo("Warmmiete\ninkl. Nebenkosten");
        assertThat(new BudgetItemNote("Warmmiete")).hasToString("Warmmiete");
    }

    /** PN-3 */
    @Test
    void missingOrBlankTextMeansNoNote() {
        assertThat(BudgetItemNote.ofNullable(null)).isNull();
        assertThat(BudgetItemNote.ofNullable("")).isNull();
        assertThat(BudgetItemNote.ofNullable(" \n ")).isNull();
        assertThat(BudgetItemNote.ofNullable(" Warmmiete ")).isEqualTo(new BudgetItemNote("Warmmiete"));

        assertThatThrownBy(() -> new BudgetItemNote(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("note");
    }

    /** PN-2 */
    @Test
    void acceptsMaximumLengthButRejectsLongerText() {
        assertThat(BudgetItemNote.ofNullable("x".repeat(BudgetItemNote.MAX_LENGTH))).isNotNull();
        assertThat(BudgetItemNote.ofNullable(" " + "x".repeat(BudgetItemNote.MAX_LENGTH) + " ")).isNotNull();

        assertThatThrownBy(() -> BudgetItemNote.ofNullable("x".repeat(BudgetItemNote.MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("note");
    }
}
