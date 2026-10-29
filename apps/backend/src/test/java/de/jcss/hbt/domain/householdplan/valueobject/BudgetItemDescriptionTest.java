package de.jcss.hbt.domain.householdplan.valueobject;

import de.jcss.hbt.domain.exception.DomainValidationException;
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
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("description");

        assertThatThrownBy(() -> BudgetItemDescription.of("  "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("description");
    }

    @Test
    void acceptsMaximumLengthButRejectsLongerText() {
        assertThat(BudgetItemDescription.of("x".repeat(BudgetItemDescription.MAX_LENGTH))).isNotNull();

        assertThatThrownBy(() -> BudgetItemDescription.of("x".repeat(BudgetItemDescription.MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("description");
    }

    @Test
    void equalityIsBasedOnTheText() {
        assertThat(BudgetItemDescription.of("Miete")).isEqualTo(BudgetItemDescription.of("Miete"));
    }
}
