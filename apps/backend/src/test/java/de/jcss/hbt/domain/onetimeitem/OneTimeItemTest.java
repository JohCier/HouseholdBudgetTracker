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
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemMoved;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemRank;
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

    private static final OneTimeItemRank RANK = OneTimeItemRank.of(BigDecimal.ONE);

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

    /** ES-10 */
    @Test
    void deleteRecordsIt() {
        OneTimeItem item = item();
        item.pullDomainEvents();

        item.delete();

        assertThat(item.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new OneTimeItemDeleted(id, Instant.now()));
    }

    /** ES-7 */
    @Test
    void aNewItemComesAfterTheItemsEnteredBefore() {
        OneTimeItem earlier = item(ItemType.EXPENSE, MARCH_15);

        assertThat(item(ItemType.EXPENSE, LocalDate.of(2026, 1, 1)).rank()).isGreaterThanOrEqualTo(earlier.rank());
        assertThat(earlier.rank()).isGreaterThan(OneTimeItemRank.of(BigDecimal.valueOf(1_000_000)));
    }

    /** ES-8 */
    @Test
    void changeKeepsThePlaceWithinTheSameTypeAndYear() {
        OneTimeItem item = stored(ItemType.EXPENSE, MARCH_15, "5");

        OneTimeItem changed = item.change(ItemType.EXPENSE, BudgetItemDescription.of("Nachzahlung"),
                money("99.00"), LocalDate.of(2026, 12, 31), BudgetItemNote.ofNullable("neu"));

        assertThat(changed.rank()).isEqualTo(OneTimeItemRank.of(new BigDecimal("5")));
    }

    /** ES-8 */
    @Test
    void changeOfTheTypeOrTheYearPutsTheItemLastInItsNewArea() {
        OneTimeItem item = stored(ItemType.EXPENSE, MARCH_15, "5");
        OneTimeItemRank last = OneTimeItemRank.last(Instant.now());

        assertThat(item.change(ItemType.INCOME, BACK_PAYMENT, money("1.00"), MARCH_15, null).rank())
                .isGreaterThanOrEqualTo(last);
        assertThat(item.change(ItemType.EXPENSE, BACK_PAYMENT, money("1.00"), LocalDate.of(2027, 3, 15), null)
                .rank()).isGreaterThanOrEqualTo(last);
    }

    /** ES-4 */
    @Test
    void moveBetweenPutsTheItemBetweenItsNeighbors() {
        OneTimeItem first = stored(ItemType.EXPENSE, MARCH_15, "1");
        OneTimeItem second = stored(ItemType.EXPENSE, MARCH_15, "2");
        OneTimeItem moved = stored(ItemType.EXPENSE, LocalDate.of(2026, 9, 22), "3");

        OneTimeItem between = moved.moveBetween(first, second);
        OneTimeItem atStart = moved.moveBetween(null, first);
        OneTimeItem atEnd = first.moveBetween(moved, null);

        assertThat(between.rank()).isGreaterThan(first.rank()).isLessThan(second.rank());
        assertThat(between.date()).isEqualTo(LocalDate.of(2026, 9, 22));
        assertThat(atStart.rank()).isLessThan(first.rank());
        assertThat(atEnd.rank()).isGreaterThan(moved.rank());
        assertThat(between.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new OneTimeItemMoved(moved.id(), Instant.now()));
    }

    /** ES-4 */
    @Test
    void moveBetweenFindsAPlaceBetweenNeighborsAgainAndAgain() {
        OneTimeItem first = stored(ItemType.EXPENSE, MARCH_15, "1");
        OneTimeItem next = stored(ItemType.EXPENSE, MARCH_15, "2");
        OneTimeItem moved = stored(ItemType.EXPENSE, MARCH_15, "3");

        for (int i = 0; i < 200; i++) {
            moved = moved.moveBetween(first, next);
            assertThat(moved.rank()).isGreaterThan(first.rank()).isLessThan(next.rank());
            next = moved;
            moved = stored(ItemType.EXPENSE, MARCH_15, "3");
        }
    }

    /** ES-4 */
    @Test
    void moveBetweenStaysWithinTheItemsOfTheSameTypeAndYear() {
        OneTimeItem item = stored(ItemType.EXPENSE, MARCH_15, "1");

        assertThatThrownBy(() -> item.moveBetween(null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("ES-4");
        assertThatThrownBy(() -> item.moveBetween(stored(ItemType.INCOME, MARCH_15, "2"), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("ES-4");
        assertThatThrownBy(() -> item.moveBetween(null, stored(ItemType.EXPENSE, LocalDate.of(2027, 1, 1), "2")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> item.moveBetween(item, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reconstituteRecordsNoEventsButChecksTheRules() {
        assertThat(OneTimeItem.reconstitute(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, money("1.00"), MARCH_15, null,
                RANK).pullDomainEvents()).isEmpty();
        assertThatThrownBy(() -> OneTimeItem.reconstitute(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, Money.ZERO,
                MARCH_15, null, RANK)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void equalityIsBasedOnTheIdOnly() {
        OneTimeItem item = item();
        OneTimeItem other = OneTimeItem.reconstitute(id, OWNER, ItemType.INCOME, BudgetItemDescription.of("Bonus"),
                money("1.00"), LocalDate.of(2027, 1, 1), null, RANK);

        assertThat(item).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(item).isNotEqualTo(OneTimeItem.reconstitute(OneTimeItemId.generate(), OWNER, ItemType.EXPENSE,
                BACK_PAYMENT, money("1200.00"), MARCH_15, null, RANK));
    }

    private OneTimeItem item() {
        return OneTimeItem.create(id, OWNER, ItemType.EXPENSE, BACK_PAYMENT, money("1200.00"), MARCH_15, null);
    }

    private static OneTimeItem item(ItemType type, LocalDate date) {
        return OneTimeItem.create(OneTimeItemId.generate(), OWNER, type, BACK_PAYMENT, money("1.00"), date, null);
    }

    private static OneTimeItem stored(ItemType type, LocalDate date, String rank) {
        return OneTimeItem.reconstitute(OneTimeItemId.generate(), OWNER, type, BACK_PAYMENT, money("1.00"), date, null,
                OneTimeItemRank.of(new BigDecimal(rank)));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
