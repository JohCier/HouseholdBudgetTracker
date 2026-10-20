package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetItemSubcategoryTest {

    /** UK-2 */
    @Test
    void removesLeadingAndTrailingWhitespace() {
        assertThat(BudgetItemSubcategory.of("  Auto ").value()).isEqualTo("Auto");
    }

    /** UK-2 */
    @Test
    void acceptsUpTo50Characters() {
        assertThat(BudgetItemSubcategory.of("a".repeat(50)).value()).hasSize(50);
    }

    /** UK-2 */
    @Test
    void rejectsLongerNamesInsteadOfShorteningThem() {
        assertThatThrownBy(() -> BudgetItemSubcategory.of("a".repeat(51)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("subcategory must not be longer than 50 characters");
    }

    /** UK-2 */
    @Test
    void rejectsBlankNames() {
        assertThatThrownBy(() -> BudgetItemSubcategory.of(" "))
                .isInstanceOf(DomainValidationException.class);
    }

    /** UK-1, UK-2 */
    @Test
    void blankOrMissingNameMeansNoSubcategory() {
        assertThat(BudgetItemSubcategory.ofNullable(null)).isNull();
        assertThat(BudgetItemSubcategory.ofNullable("  ")).isNull();
        assertThat(BudgetItemSubcategory.ofNullable(" Auto")).isEqualTo(BudgetItemSubcategory.of("Auto"));
    }

    /** UK-1 */
    @Test
    void itemsWithoutSubcategoryBelongToOther() {
        assertThat(BudgetItemSubcategory.groupOf(null)).isEqualTo(BudgetItemSubcategory.OTHER);
        assertThat(BudgetItemSubcategory.groupOf(BudgetItemSubcategory.of("Auto")))
                .isEqualTo(BudgetItemSubcategory.of("Auto"));
        assertThat(BudgetItemSubcategory.OTHER.value()).isEqualTo("Sonstiges");
    }

    /** UK-5 */
    @Test
    void sameGroupIgnoresCase() {
        assertThat(BudgetItemSubcategory.of("auto").isSameAs(BudgetItemSubcategory.of("AUTO"))).isTrue();
        assertThat(BudgetItemSubcategory.of("Auto").isSameAs(BudgetItemSubcategory.of("Autos"))).isFalse();
        assertThat(BudgetItemSubcategory.of("sonstiges").isOther()).isTrue();
        assertThat(BudgetItemSubcategory.of("auto")).isNotEqualTo(BudgetItemSubcategory.of("Auto"));
    }
}
