package de.jcss.hbt.domain.onetimeitem;

import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemChanged;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemCreated;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemDeleted;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneTimeItemTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final LocalDate MARCH_15 = LocalDate.of(2026, 3, 15);
    private static final BudgetItemDescription BACK_PAYMENT = BudgetItemDescription.of("Nebenkostennachzahlung");

    private final OneTimeItemId id = OneTimeItemId.generate();

    /** EP-1, EP-3 */
    @Test
    void createKeepsWhatWasEnteredAndRecordsIt() {
        OneTimeItem item = OneTimeItem.create(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, money("1200.00"), MARCH_15,
                BudgetItemNote.ofNullable("Abrechnung 2025"));

        assertThat(item.id()).isEqualTo(id);
        assertThat(item.isOwnedBy(OWNER)).isTrue();
        assertThat(item.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(item.description()).isEqualTo(BACK_PAYMENT);
        assertThat(item.amount()).isEqualTo(money("1200.00"));
        assertThat(item.date()).isEqualTo(MARCH_15);
        assertThat(item.year()).isEqualTo(Year.of(2026));
        assertThat(item.note()).isEqualTo(new BudgetItemNote("Abrechnung 2025"));
        assertThat(item.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new OneTimeItemCreated(id, OWNER, MARCH_15, Instant.now()));
    }

    /** EP-1 */
    @Test
    void theNoteIsOptional() {
        assertThat(item().note()).isNull();
    }

    /** EP-4 */
    @Test
    void belongsOnlyToItsOwner() {
        assertThat(item().isOwnedBy(OwnerId.of(UUID.randomUUID()))).isFalse();
    }

    /** EP-1 */
    @Test
    void rejectsAnAmountThatIsNotPositive() {
        assertThatThrownBy(() -> OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, Money.ZERO, MARCH_15, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("amount must be positive");
        assertThatThrownBy(() -> OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, money("-1.00"), MARCH_15, null))
                .isInstanceOf(DomainValidationException.class);
    }

    /** EP-3 */
    @Test
    void acceptsTheDaysFrom1900To2200() {
        assertThat(OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, money("1.00"),
                LocalDate.of(1900, 1, 1), null).year()).isEqualTo(Year.of(1900));
        assertThat(OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, money("1.00"),
                LocalDate.of(2200, 12, 31), null).year()).isEqualTo(Year.of(2200));
    }

    /** EP-3 */
    @Test
    void rejectsDaysBefore1900OrAfter2200() {
        assertThatThrownBy(() -> OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, money("1.00"),
                LocalDate.of(1899, 12, 31), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("date must be between 1900-01-01 and 2200-12-31, but was 1899-12-31");
        assertThatThrownBy(() -> OneTimeItem.create(id, OWNER, ItemType.INCOME, BACK_PAYMENT, money("1.00"),
                LocalDate.of(2201, 1, 1), null))
                .isInstanceOf(DomainValidationException.class);
    }

    /** EP-7 */
    @Test
    void changeReplacesEverythingAndMovesTheItemToTheYearOfItsNewDate() {
        OneTimeItem item = item();
        item.pullDomainEvents();
        LocalDate nextYear = LocalDate.of(2027, 1, 5);

        OneTimeItem changed = item.change(ItemType.INCOME, BudgetItemDescription.of("Steuererstattung"),
                money("800.00"), nextYear, BudgetItemNote.ofNullable("Bescheid"));

        assertThat(changed).isEqualTo(item);
        assertThat(changed.type()).isEqualTo(ItemType.INCOME);
        assertThat(changed.description()).isEqualTo(BudgetItemDescription.of("Steuererstattung"));
        assertThat(changed.amount()).isEqualTo(money("800.00"));
        assertThat(changed.year()).isEqualTo(Year.of(2027));
        assertThat(changed.note()).isEqualTo(new BudgetItemNote("Bescheid"));
        assertThat(changed.isOwnedBy(OWNER)).isTrue();
        assertThat(changed.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new OneTimeItemChanged(id, nextYear, Instant.now()));
    }

    /** EP-7 */
    @Test
    void changeChecksTheSameRulesAndLeavesTheItemAsItWas() {
        OneTimeItem item = item();

        assertThatThrownBy(() -> item.change(ItemType.EXPENSE, BACK_PAYMENT, Money.ZERO, MARCH_15, null))
                .isInstanceOf(DomainValidationException.class);
        assertThat(item.amount()).isEqualTo(money("1200.00"));
    }

    /** EP-7 */
    @Test
    void changeCarriesOverPendingEvents() {
        OneTimeItem changed = item().change(ItemType.EXPENSE, BACK_PAYMENT, money("1300.00"), MARCH_15, null);

        assertThat(changed.pullDomainEvents())
                .extracting(Object::getClass)
                .containsExactly(OneTimeItemCreated.class, OneTimeItemChanged.class);
    }

    /** EP-17 */
    @Test
    void deleteRecordsIt() {
        OneTimeItem item = item();
        item.pullDomainEvents();

        item.delete();

        assertThat(item.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new OneTimeItemDeleted(id, Instant.now()));
    }

    @Test
    void reconstituteRecordsNoEventsButChecksTheRules() {
        assertThat(OneTimeItem.reconstitute(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, money("1.00"), MARCH_15, null)
                .pullDomainEvents()).isEmpty();
        assertThatThrownBy(() -> OneTimeItem.reconstitute(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, Money.ZERO,
                MARCH_15, null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void equalityIsBasedOnTheIdOnly() {
        OneTimeItem item = item();
        OneTimeItem other = OneTimeItem.reconstitute(id, OWNER, ItemType.INCOME, BudgetItemDescription.of("Bonus"),
                money("1.00"), LocalDate.of(2027, 1, 1), null);

        assertThat(item).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(item).isNotEqualTo(OneTimeItem.reconstitute(OneTimeItemId.generate(), OWNER, ItemType.EXPENSE,
                BACK_PAYMENT, money("1200.00"), MARCH_15, null));
    }

    private OneTimeItem item() {
        return OneTimeItem.create(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, money("1200.00"), MARCH_15, null);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
