package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCopied;
import de.jcss.hbt.domain.householdplan.event.HouseholdPlanCreated;
import de.jcss.hbt.domain.householdplan.valueobject.AmountPeriod;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Taking over a plan as template for another year (PU-1 to PU-11). */
class HouseholdPlanCopyTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final ItemSection LIVING = ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT);
    private static final BudgetItemSubcategory CAR = BudgetItemSubcategory.of("Auto");
    private static final BudgetItemSubcategory HOME = BudgetItemSubcategory.of("Wohnen");

    private final ItemAmount amount = ItemAmount.monthly(Money.of(new BigDecimal("100.00")));
    private final HouseholdPlan source = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));

    @BeforeEach
    void fillSource() {
        ItemAmount salary = ItemAmount.yearly(Money.of(new BigDecimal("36000.00")));
        source.addIncome(BudgetItemDescription.of("Gehalt"), salary, null, null);
        source.addExpense(BudgetItemDescription.of("Wartung"), amount, ExpenseCategory.LEBENSUNTERHALT, CAR,
                BudgetItemNote.ofNullable("Inspektion im Mai"));
        source.addExpense(BudgetItemDescription.of("Reifen"), amount, ExpenseCategory.LEBENSUNTERHALT, CAR, null);
        source.addExpense(BudgetItemDescription.of("Miete"), amount, ExpenseCategory.LEBENSUNTERHALT, HOME, null);
        source.reorderSubcategories(LIVING, List.of(HOME, CAR));
        source.pullDomainEvents();
    }

    /** PU-3, PU-4, PU-8 */
    @Test
    void createFromCopiesAllItemsInTheirOrder() {
        HouseholdPlanId id = HouseholdPlanId.generate();

        HouseholdPlan copy = HouseholdPlan.createFrom(source, id, Year.of(2027));

        assertThat(copy.id()).isEqualTo(id);
        assertThat(copy.year()).isEqualTo(Year.of(2027));
        assertThat(copy.isOwnedBy(OWNER)).isTrue();
        assertSameItems(copy, source);
        assertThat(copy.subcategoryOrders()).isEqualTo(source.subcategoryOrders());
        assertThat(copy.summary()).isEqualTo(source.summary());
    }

    /** PU-3, MJ-1: monthly items stay monthly, yearly items stay yearly */
    @Test
    void createFromKeepsTheAmountWithItsPeriod() {
        HouseholdPlan copy = HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), Year.of(2027));

        assertThat(copy.items()).extracting(BudgetItem::amount)
                .containsExactlyElementsOf(source.items().stream().map(BudgetItem::amount).toList())
                .extracting(ItemAmount::period)
                .contains(AmountPeriod.MONTHLY, AmountPeriod.YEARLY);
    }

    /** PU-5 */
    @Test
    void copiedItemsAreItemsOfTheirOwn() {
        HouseholdPlan copy = HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), Year.of(2027));

        assertThat(copy.items()).extracting(BudgetItem::id).doesNotContainAnyElementsOf(
                source.items().stream().map(BudgetItem::id).toList());
        copy.removeItem(copy.items().getFirst().id());
        assertThat(source.items()).hasSize(4);
        assertThat(source.pullDomainEvents()).isEmpty();
    }

    /** PU-11 */
    @Test
    void createFromRecordsCreationAndCopyOnly() {
        HouseholdPlanId id = HouseholdPlanId.generate();

        HouseholdPlan copy = HouseholdPlan.createFrom(source, id, Year.of(2027));

        assertThat(copy.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new HouseholdPlanCreated(id, OWNER, Year.of(2027), Instant.now()),
                        new HouseholdPlanCopied(id, source.id(), false, Instant.now()));
    }

    /** PU-2 */
    @Test
    void createFromAcceptsEarlierYears() {
        assertThat(HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), Year.of(2020)).items()).hasSize(4);
    }

    /** PU-2, HP-1 */
    @Test
    void createFromRejectsTheYearOfTheSourceAndYearsOutOfRange() {
        assertThatThrownBy(() -> HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), Year.of(2026)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("the target year must differ from the year of the source, 2026");
        assertThatThrownBy(() -> HouseholdPlan.createFrom(source, HouseholdPlanId.generate(), Year.of(2201)))
                .isInstanceOf(DomainValidationException.class);
    }

    /** PU-6 */
    @Test
    void anEmptyPlanGivesAnEmptyCopy() {
        HouseholdPlan empty = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2030));

        assertThat(HouseholdPlan.createFrom(empty, HouseholdPlanId.generate(), Year.of(2031)).items()).isEmpty();
    }

    /** PU-9, PU-11 */
    @Test
    void overwriteWithReplacesAllItemsAndTheOrder() {
        HouseholdPlanId targetId = HouseholdPlanId.generate();
        HouseholdPlan target = HouseholdPlan.create(targetId, OWNER, Year.of(2027));
        BudgetItemId old = target.addExpense(BudgetItemDescription.of("Alt"), amount, ExpenseCategory.FREIZEIT_SPASS,
                null, null);
        target.reorderSubcategories(ItemSection.expenses(ExpenseCategory.FREIZEIT_SPASS),
                List.of(BudgetItemSubcategory.OTHER));
        target.pullDomainEvents();

        target.overwriteWith(source);

        assertThat(target.id()).isEqualTo(targetId);
        assertThat(target.year()).isEqualTo(Year.of(2027));
        assertThat(target.items()).extracting(BudgetItem::id).doesNotContain(old);
        assertSameItems(target, source);
        assertThat(target.subcategoryOrders()).isEqualTo(source.subcategoryOrders());
        assertThat(target.pullDomainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new HouseholdPlanCopied(targetId, source.id(), true, Instant.now()));
    }

    /** PU-2 */
    @Test
    void aPlanCannotBeOverwrittenWithItself() {
        assertThatThrownBy(() -> source.overwriteWith(source))
                .isInstanceOf(DomainValidationException.class);
        assertThat(source.items()).hasSize(4);
    }

    /** HP-4 */
    @Test
    void onlyPlansOfTheSameOwnerAreCopied() {
        HouseholdPlan foreign = HouseholdPlan.create(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()),
                Year.of(2027));

        assertThatThrownBy(() -> foreign.overwriteWith(source)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertSameItems(HouseholdPlan copy, HouseholdPlan original) {
        assertThat(copy.items()).hasSameSizeAs(original.items());
        for (int i = 0; i < original.items().size(); i++) {
            assertThat(copy.items().get(i).hasSameValuesAs(original.items().get(i))).isTrue();
        }
    }
}
