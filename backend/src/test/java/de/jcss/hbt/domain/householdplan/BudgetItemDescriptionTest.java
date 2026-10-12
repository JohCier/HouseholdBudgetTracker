package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemDescriptionTest {

    @Test
    void keepsTheText() {
        assertThat(BudgetItemDescription.of("Miete").value()).isEqualTo("Miete");
        assertThat(BudgetItemDescription.of("Miete")).hasToString("Miete");
    }

    @Test
    void rejectsMissingOrBlankText() {
        assertThatThrownBy(() -> BudgetItemDescription.of(null))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");

        assertThatThrownBy(() -> BudgetItemDescription.of("  "))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void acceptsMaximumLengthButRejectsLongerText() {
        assertThat(BudgetItemDescription.of("x".repeat(BudgetItemDescription.MAX_LENGTH))).isNotNull();

        assertThatThrownBy(() -> BudgetItemDescription.of("x".repeat(BudgetItemDescription.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidBudgetItemException.class)
                .hasMessageContaining("description");
    }

    @Test
    void equalityIsBasedOnTheText() {
        assertThat(BudgetItemDescription.of("Miete")).isEqualTo(BudgetItemDescription.of("Miete"));
    }
}
