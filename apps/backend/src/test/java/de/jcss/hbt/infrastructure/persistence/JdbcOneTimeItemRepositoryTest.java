package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItem;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeOverviewItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Stores one-time items and reads them back, through the repository and through the query handler of a year. */
@IntegrationTest
@Transactional
class JdbcOneTimeItemRepositoryTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());

    @Autowired
    private JdbcOneTimeItemRepository repository;

    @Autowired
    private JdbcOneTimeItemOverviewQueryHandler queryHandler;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM one_time_item").update();
    }

    @Test
    void storesAndLoadsAnItem() { // EP-1
        OneTimeItem item = item("Nebenkostennachzahlung", ItemType.EXPENSE, "1200.00", LocalDate.of(2026, 3, 15),
                "Abrechnung 2025");
        repository.add(item);

        OneTimeItem loaded = repository.findById(item.id()).orElseThrow();

        assertThat(loaded.isOwnedBy(OWNER)).isTrue();
        assertThat(loaded.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(loaded.description()).isEqualTo(BudgetItemDescription.of("Nebenkostennachzahlung"));
        assertThat(loaded.amount()).isEqualTo(money("1200.00"));
        assertThat(loaded.date()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(loaded.note()).isEqualTo(new BudgetItemNote("Abrechnung 2025"));
        assertThat(loaded.pullDomainEvents()).isEmpty();
    }

    @Test
    void savesAChange() { // EP-7
        OneTimeItem item = item("Bonus", ItemType.INCOME, "2000.00", LocalDate.of(2026, 12, 1), "Jahresprämie");
        repository.add(item);

        repository.save(repository.findById(item.id()).orElseThrow()
                .change(ItemType.EXPENSE, BudgetItemDescription.of("Reparatur"), money("650.00"),
                        LocalDate.of(2027, 1, 5), null));

        OneTimeItem loaded = repository.findById(item.id()).orElseThrow();
        assertThat(loaded.type()).isEqualTo(ItemType.EXPENSE);
        assertThat(loaded.description()).isEqualTo(BudgetItemDescription.of("Reparatur"));
        assertThat(loaded.amount()).isEqualTo(money("650.00"));
        assertThat(loaded.year()).isEqualTo(Year.of(2027));
        assertThat(loaded.note()).isNull();
    }

    @Test
    void removesAnItem() { // EP-17
        OneTimeItem item = item("Bonus", ItemType.INCOME, "2000.00", LocalDate.of(2026, 12, 1), null);
        repository.add(item);

        repository.remove(item);

        assertThat(repository.findById(item.id())).isEmpty();
    }

    @Test
    void unknownItemIsNotFound() {
        assertThat(repository.findById(OneTimeItemId.generate())).isEmpty();
    }

    @Test
    void overviewListsTheItemsOfTheYearByDateAndThenInTheOrderEntered() { // EP-8, EP-9
        repository.add(item("Später", ItemType.INCOME, "800.00", LocalDate.of(2026, 7, 10), null));
        repository.add(item("Zuerst erfasst", ItemType.EXPENSE, "1200.00", LocalDate.of(2026, 3, 15), "Notiz"));
        repository.add(item("Danach erfasst", ItemType.EXPENSE, "100.00", LocalDate.of(2026, 3, 15), null));
        repository.add(item("Anderes Jahr", ItemType.INCOME, "1.00", LocalDate.of(2027, 1, 1), null));
        repository.add(OneTimeItem.create(OneTimeItemId.generate(), OwnerId.of(UUID.randomUUID()), ItemType.INCOME,
                BudgetItemDescription.of("Fremd"), money("1.00"), LocalDate.of(2026, 5, 1), null));

        OneTimeItemOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026));

        assertThat(overview.items()).extracting(item -> item.description().value())
                .containsExactly("Zuerst erfasst", "Danach erfasst", "Später");
        assertThat(overview.items().getFirst().note()).isEqualTo(new BudgetItemNote("Notiz"));
        assertThat(overview.totalIncome()).isEqualTo(money("800.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("1300.00"));
        assertThat(overview.balance()).isEqualTo(money("-500.00"));
    }

    @Test
    void overviewOfAYearWithoutItemsIsEmpty() { // EP-10
        OneTimeItemOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026));

        assertThat(overview.items()).isEmpty();
        assertThat(overview.balance()).isEqualTo(Money.ZERO);
    }

    @Test
    void overviewIncludesTheFirstAndLastDayOfTheYear() { // EP-3
        repository.add(item("Erster", ItemType.INCOME, "1.00", LocalDate.of(2026, 1, 1), null));
        repository.add(item("Letzter", ItemType.INCOME, "1.00", LocalDate.of(2026, 12, 31), null));

        assertThat(queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).items())
                .extracting(OneTimeOverviewItem::date)
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    private static OneTimeItem item(String description, ItemType type, String amount, LocalDate date, String note) {
        return OneTimeItem.create(OneTimeItemId.generate(), OWNER, type, BudgetItemDescription.of(description),
                money(amount), date, BudgetItemNote.ofNullable(note));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
