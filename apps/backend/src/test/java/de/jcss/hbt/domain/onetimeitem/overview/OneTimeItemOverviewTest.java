package de.jcss.hbt.domain.onetimeitem.overview;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimeItemOverviewTest {

    /** EP-9 */
    @Test
    void sumsTheIncomesAndExpensesOfTheYear() {
        OneTimeItemOverview overview = new OneTimeItemOverview(Year.of(2026), List.of(
                item(ItemType.EXPENSE, "Nebenkostennachzahlung", "1200.00", LocalDate.of(2026, 3, 15)),
                item(ItemType.INCOME, "Steuererstattung", "800.00", LocalDate.of(2026, 7, 10))));

        assertThat(overview.totalIncome()).isEqualTo(money("800.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("1200.00"));
        assertThat(overview.balance()).isEqualTo(money("-400.00"));
        assertThat(overview.expensesExceedIncome()).isTrue();
    }

    /** EP-9, EP-10 */
    @Test
    void aYearWithoutItemsHasZeroTotals() {
        OneTimeItemOverview overview = new OneTimeItemOverview(Year.of(2027), List.of());

        assertThat(overview.totalIncome()).isEqualTo(Money.ZERO);
        assertThat(overview.totalExpense()).isEqualTo(Money.ZERO);
        assertThat(overview.balance()).isEqualTo(Money.ZERO);
        assertThat(overview.expensesExceedIncome()).isFalse();
    }

    private static OneTimeOverviewItem item(ItemType type, String description, String amount, LocalDate date) {
        return new OneTimeOverviewItem(OneTimeItemId.generate(), type, BudgetItemDescription.of(description),
                money(amount), date, null);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
