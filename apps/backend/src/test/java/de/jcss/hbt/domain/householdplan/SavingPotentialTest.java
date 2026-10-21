package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavingPotentialTest {

    /** PM-2a */
    @Test
    void mustBePositive() {
        assertThatThrownBy(() -> SavingPotential.monthly(money("0.00")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("saving potential must be positive");
        assertThatThrownBy(() -> SavingPotential.monthly(money("-1.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    /** PM-13 */
    @Test
    void forTheYearIsTwelveTimesTheMonthlyAmount() {
        assertThat(SavingPotential.monthly(money("5.25")).annual()).isEqualTo(money("63.00"));
    }

    /** PM-2a: compared with the monthly amount of the expense, also of a yearly one */
    @Test
    void exceedsAnAmountAboveItsMonthlyAmount() {
        ItemAmount yearly = ItemAmount.yearly(money("360.00"));

        assertThat(SavingPotential.monthly(money("30.00")).exceeds(yearly)).isFalse();
        assertThat(SavingPotential.monthly(money("30.01")).exceeds(yearly)).isTrue();
    }

    /** PM-5 */
    @Test
    void isCappedToTheMonthlyAmount() {
        SavingPotential potential = SavingPotential.monthly(money("5.00"));

        assertThat(potential.cappedTo(ItemAmount.monthly(money("20.00")))).isSameAs(potential);
        assertThat(potential.cappedTo(ItemAmount.yearly(money("36.00"))))
                .isEqualTo(SavingPotential.monthly(money("3.00")));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
