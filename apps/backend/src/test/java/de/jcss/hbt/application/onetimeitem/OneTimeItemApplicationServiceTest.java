package de.jcss.hbt.application.onetimeitem;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.OneTimeItemNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItem;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemRepository;
import de.jcss.hbt.domain.onetimeitem.command.ChangeOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.CreateOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.DeleteOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.MoveOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemChanged;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemCreated;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemDeleted;
import de.jcss.hbt.domain.onetimeitem.event.OneTimeItemMoved;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverviewQueryHandler;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeOverviewItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link OneTimeItemApplicationService} loads, saves and publishes correctly, using an
 * in-memory fake repository whose items the fake query handler reads.
 */
class OneTimeItemApplicationServiceTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final OwnerId SOMEONE_ELSE = OwnerId.of(UUID.randomUUID());
    private static final BudgetItemDescription BACK_PAYMENT = BudgetItemDescription.of("Nebenkostennachzahlung");
    private static final LocalDate MARCH_15 = LocalDate.of(2026, 3, 15);

    private final FakeRepository repository = new FakeRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final OneTimeItemApplicationService service =
            new OneTimeItemApplicationService(repository, repository.queryHandler(), publishedEvents::add);

    /** EP-1, EP-5 */
    @Test
    void createItemStoresItAndPublishesIt() {
        OneTimeItemId id = create(OWNER, MARCH_15);

        assertThat(repository.items.get(id).isOwnedBy(OWNER)).isTrue();
        assertThat(publishedEvents).singleElement().isInstanceOf(OneTimeItemCreated.class);
    }

    /** EP-8, EP-9 */
    @Test
    void overviewListsTheItemsOfTheYearOfTheUserOnly() {
        create(OWNER, MARCH_15);
        create(OWNER, LocalDate.of(2027, 1, 1));
        create(SOMEONE_ELSE, MARCH_15);

        OneTimeItemOverview overview = service.getOverview(OWNER, Year.of(2026));

        assertThat(overview.items()).singleElement().extracting(OneTimeOverviewItem::date).isEqualTo(MARCH_15);
        assertThat(overview.totalExpense()).isEqualTo(money("1200.00"));
    }

    /** EP-7 */
    @Test
    void changeItemSavesTheChangeAndPublishesIt() {
        OneTimeItemId id = create(OWNER, MARCH_15);
        publishedEvents.clear();

        service.changeItem(new ChangeOneTimeItemCommand(OWNER, id, ItemType.INCOME,
                BudgetItemDescription.of("Steuererstattung"), money("800.00"), LocalDate.of(2027, 1, 5),
                BudgetItemNote.ofNullable("Bescheid")));

        OneTimeItem stored = repository.items.get(id);
        assertThat(stored.type()).isEqualTo(ItemType.INCOME);
        assertThat(stored.year()).isEqualTo(Year.of(2027));
        assertThat(stored.note()).isEqualTo(new BudgetItemNote("Bescheid"));
        assertThat(publishedEvents).singleElement().isInstanceOf(OneTimeItemChanged.class);
        assertThat(service.getOverview(OWNER, Year.of(2026)).items()).isEmpty();
        assertThat(service.getOverview(OWNER, Year.of(2027)).items()).hasSize(1);
    }

    /** EP-7 */
    @Test
    void anInvalidChangeIsNotSaved() {
        OneTimeItemId id = create(OWNER, MARCH_15);

        assertThatThrownBy(() -> service.changeItem(new ChangeOneTimeItemCommand(OWNER, id, ItemType.EXPENSE,
                BACK_PAYMENT, money("1.00"), LocalDate.of(2201, 1, 1), null)))
                .isInstanceOf(DomainValidationException.class);
        assertThat(repository.items.get(id).date()).isEqualTo(MARCH_15);
    }

    /** ES-4 */
    @Test
    void moveItemChangesOnlyTheMovedItemAndPublishesIt() {
        OneTimeItemId first = create(OWNER, MARCH_15);
        OneTimeItemId second = create(OWNER, MARCH_15);
        OneTimeItemId moved = create(OWNER, MARCH_15);
        OneTimeItem firstBefore = repository.items.get(first);
        OneTimeItem secondBefore = repository.items.get(second);
        publishedEvents.clear();

        service.moveItem(new MoveOneTimeItemCommand(OWNER, moved, null, first));

        assertThat(repository.items.get(moved).rank()).isLessThan(repository.items.get(first).rank());
        assertThat(repository.items.get(first).rank()).isEqualTo(firstBefore.rank());
        assertThat(repository.items.get(second).rank()).isEqualTo(secondBefore.rank());
        assertThat(publishedEvents).singleElement().isInstanceOf(OneTimeItemMoved.class);
    }

    /** ES-4, EP-4 */
    @Test
    void anItemCannotBeMovedNextToAnItemOfSomeoneElse() {
        OneTimeItemId own = create(OWNER, MARCH_15);
        OneTimeItemId foreign = create(SOMEONE_ELSE, MARCH_15);
        OneTimeItem before = repository.items.get(own);

        assertThatThrownBy(() -> service.moveItem(new MoveOneTimeItemCommand(OWNER, own, foreign, null)))
                .isInstanceOf(OneTimeItemNotFoundException.class);
        assertThatThrownBy(() -> service.moveItem(new MoveOneTimeItemCommand(OWNER, foreign, null, own)))
                .isInstanceOf(OneTimeItemNotFoundException.class);
        assertThat(repository.items.get(own).rank()).isEqualTo(before.rank());
    }

    /** ES-10 */
    @Test
    void deleteItemRemovesItAndPublishesIt() {
        OneTimeItemId id = create(OWNER, MARCH_15);
        publishedEvents.clear();

        service.deleteItem(new DeleteOneTimeItemCommand(OWNER, id));

        assertThat(repository.items).doesNotContainKey(id);
        assertThat(publishedEvents).singleElement().isInstanceOf(OneTimeItemDeleted.class);
    }

    /** EP-4 */
    @Test
    void anItemOfSomeoneElseIsTreatedAsMissing() {
        OneTimeItemId id = create(SOMEONE_ELSE, MARCH_15);

        assertThatThrownBy(() -> service.changeItem(new ChangeOneTimeItemCommand(OWNER, id, ItemType.EXPENSE,
                BACK_PAYMENT, money("1.00"), MARCH_15, null)))
                .isInstanceOf(OneTimeItemNotFoundException.class);
        assertThatThrownBy(() -> service.deleteItem(new DeleteOneTimeItemCommand(OWNER, id)))
                .isInstanceOf(OneTimeItemNotFoundException.class);
        assertThat(repository.items).containsKey(id);
    }

    @Test
    void anUnknownItemIsReportedAsMissing() {
        assertThatThrownBy(() -> service.deleteItem(new DeleteOneTimeItemCommand(OWNER, OneTimeItemId.generate())))
                .isInstanceOf(OneTimeItemNotFoundException.class);
    }

    private OneTimeItemId create(OwnerId owner, LocalDate date) {
        return service.createItem(new CreateOneTimeItemCommand(owner, ItemType.EXPENSE, BACK_PAYMENT,
                money("1200.00"), date, null));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static final class FakeRepository implements OneTimeItemRepository {

        private final Map<OneTimeItemId, OneTimeItem> items = new HashMap<>();

        @Override
        public Optional<OneTimeItem> findById(OneTimeItemId id) {
            return Optional.ofNullable(items.get(id)).map(FakeRepository::copyOf);
        }

        @Override
        public void add(OneTimeItem item) {
            save(item);
        }

        @Override
        public void save(OneTimeItem item) {
            items.put(item.id(), copyOf(item));
        }

        @Override
        public void remove(OneTimeItem item) {
            items.remove(item.id());
        }

        private static OneTimeItem copyOf(OneTimeItem item) {
            return OneTimeItem.reconstitute(item.id(), item.owner(), item.type(), item.description(), item.amount(),
                    item.date(), item.note(), item.rank());
        }

        OneTimeItemOverviewQueryHandler queryHandler() {
            return (owner, year) -> new OneTimeItemOverview(year, items.values().stream()
                    .filter(item -> item.isOwnedBy(owner) && item.year().equals(year))
                    .sorted(Comparator.comparing(OneTimeItem::rank))
                    .map(item -> new OneTimeOverviewItem(item.id(), item.type(), item.description(), item.amount(),
                            item.date(), item.note()))
                    .toList());
        }
    }
}
