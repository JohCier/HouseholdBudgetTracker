package de.jcss.hbt.domain.networth;

import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.NetWorthEntryNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.networth.event.NetWorthEntryAdded;
import de.jcss.hbt.domain.networth.event.NetWorthEntryChanged;
import de.jcss.hbt.domain.networth.event.NetWorthEntryRemoved;
import de.jcss.hbt.domain.networth.event.NetWorthStatementCopied;
import de.jcss.hbt.domain.networth.event.NetWorthStatementCreated;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryId;
import de.jcss.hbt.domain.networth.valueobject.NetWorthEntryName;
import de.jcss.hbt.domain.networth.valueobject.NetWorthStatementId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

import static de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType.ASSET;
import static de.jcss.hbt.domain.networth.valueobject.NetWorthEntryType.LIABILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** Assets and liabilities of a year (VS-1 to VS-7, VS-15, VS-16). */
class NetWorthStatementTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());

    private final NetWorthStatement statement = NetWorthStatement.create(NetWorthStatementId.generate(), OWNER,
            Year.of(2026));

    /** VS-7 */
    @Test
    void createRecordsTheCreationAndStartsEmpty() {
        assertThat(statement.isEmpty()).isTrue();
        assertThat(statement.isOwnedBy(OWNER)).isTrue();
        assertThat(statement.pullDomainEvents()).singleElement()
                .isInstanceOfSatisfying(NetWorthStatementCreated.class, event -> {
                    assertThat(event.owner()).isEqualTo(OWNER);
                    assertThat(event.year()).isEqualTo(Year.of(2026));
                });
    }

    /** VS-2 */
    @Test
    void onlyYearsFrom1900To2200() {
        assertThatThrownBy(() -> NetWorthStatement.create(NetWorthStatementId.generate(), OWNER, Year.of(1899)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> NetWorthStatement.create(NetWorthStatementId.generate(), OWNER, Year.of(2201)))
                .isInstanceOf(DomainValidationException.class);
        assertThat(NetWorthStatement.create(NetWorthStatementId.generate(), OWNER, Year.of(2200)).year())
                .isEqualTo(Year.of(2200));
    }

    /** VS-1, VS-5, VS-8 */
    @Test
    void addEntryKeepsTheOrderOfRecording() {
        statement.pullDomainEvents();
        NetWorthEntryId account = statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        statement.addEntry(LIABILITY, name("Autokredit"), money("6300.00"));
        statement.addEntry(ASSET, name("ETF-Depot"), money("12500.00"));

        assertThat(statement.entries()).extracting(entry -> entry.name().value())
                .containsExactly("Girokonto", "Autokredit", "ETF-Depot");
        assertThat(statement.entries().getFirst().id()).isEqualTo(account);
        assertThat(statement.pullDomainEvents()).hasSize(3).allMatch(NetWorthEntryAdded.class::isInstance);
    }

    /** VS-1: liabilities are recorded with a positive amount too */
    @Test
    void amountMustBePositive() {
        assertThatThrownBy(() -> statement.addEntry(LIABILITY, name("Autokredit"), money("0.00")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> statement.addEntry(ASSET, name("Girokonto"), money("-1.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    /** VS-1 */
    @Test
    void nameMustNotBeBlankNorTooLong() {
        assertThatThrownBy(() -> name(" ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> name("x".repeat(256))).isInstanceOf(DomainValidationException.class);
        assertThat(name("x".repeat(255)).value()).hasSize(255);
    }

    /** VS-5 */
    @Test
    void changeEntryKeepsItsIdAndPosition() {
        NetWorthEntryId account = statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        statement.addEntry(ASSET, name("Tagesgeld"), money("6000.00"));
        statement.pullDomainEvents();

        statement.changeEntry(account, LIABILITY, name("Dispo"), money("500.00"));

        NetWorthEntry changed = statement.entries().getFirst();
        assertThat(changed.id()).isEqualTo(account);
        assertThat(changed.type()).isEqualTo(LIABILITY);
        assertThat(changed.name()).isEqualTo(name("Dispo"));
        assertThat(changed.amount()).isEqualTo(money("500.00"));
        assertThat(statement.pullDomainEvents()).singleElement().isInstanceOf(NetWorthEntryChanged.class);
    }

    @Test
    void changeWithoutDifferenceRecordsNoEvent() {
        NetWorthEntryId account = statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        statement.pullDomainEvents();

        statement.changeEntry(account, ASSET, name("Girokonto"), money("3200"));

        assertThat(statement.pullDomainEvents()).isEmpty();
    }

    /** VS-5, VS-7 */
    @Test
    void removingTheLastEntryLeavesTheStatementEmpty() {
        NetWorthEntryId account = statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        statement.pullDomainEvents();

        statement.removeEntry(account);

        assertThat(statement.isEmpty()).isTrue();
        assertThat(statement.pullDomainEvents()).singleElement().isInstanceOf(NetWorthEntryRemoved.class);
    }

    @Test
    void unknownEntriesAreNotFound() {
        NetWorthEntryId unknown = NetWorthEntryId.generate();

        assertThatThrownBy(() -> statement.removeEntry(unknown)).isInstanceOf(NetWorthEntryNotFoundException.class);
        assertThatThrownBy(() -> statement.changeEntry(unknown, ASSET, name("Girokonto"), money("1.00")))
                .isInstanceOf(NetWorthEntryNotFoundException.class);
    }

    /** VS-15 */
    @Test
    void createFromCopiesAllEntriesAsEntriesOfTheirOwn() {
        statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        statement.addEntry(LIABILITY, name("Autokredit"), money("6300.00"));
        statement.pullDomainEvents();

        NetWorthStatement copy = NetWorthStatement.createFrom(statement, NetWorthStatementId.generate(), Year.of(2027));

        assertThat(copy.year()).isEqualTo(Year.of(2027));
        assertThat(copy.isOwnedBy(OWNER)).isTrue();
        assertThat(copy.entries()).extracting(NetWorthEntry::type, NetWorthEntry::name, NetWorthEntry::amount)
                .containsExactlyElementsOf(statement.entries().stream()
                        .map(entry -> tuple(entry.type(), entry.name(), entry.amount()))
                        .toList());
        assertThat(copy.entries()).extracting(NetWorthEntry::id)
                .doesNotContainAnyElementsOf(statement.entries().stream().map(NetWorthEntry::id).toList());
        assertThat(copy.pullDomainEvents()).hasSize(2)
                .anyMatch(NetWorthStatementCreated.class::isInstance)
                .anySatisfy(event -> assertThat(event).isInstanceOfSatisfying(NetWorthStatementCopied.class,
                        copied -> assertThat(copied.overwritten()).isFalse()));

        copy.removeEntry(copy.entries().getFirst().id());
        assertThat(statement.entries()).hasSize(2);
        assertThat(statement.pullDomainEvents()).isEmpty();
    }

    /** VS-16 */
    @Test
    void overwriteWithReplacesAllEntries() {
        statement.addEntry(ASSET, name("Girokonto"), money("3200.00"));
        NetWorthStatement target = NetWorthStatement.create(NetWorthStatementId.generate(), OWNER, Year.of(2027));
        target.addEntry(ASSET, name("Sparbuch"), money("100.00"));
        target.addEntry(ASSET, name("Bargeld"), money("50.00"));
        target.pullDomainEvents();

        target.overwriteWith(statement);

        assertThat(target.entries()).extracting(entry -> entry.name().value()).containsExactly("Girokonto");
        assertThat(target.pullDomainEvents()).singleElement()
                .isInstanceOfSatisfying(NetWorthStatementCopied.class, copied -> {
                    assertThat(copied.overwritten()).isTrue();
                    assertThat(copied.sourceStatementId()).isEqualTo(statement.id());
                });
    }

    @Test
    void theTargetYearMustDifferAndTheSourceMustBeOwnedByTheSameUser() {
        assertThatThrownBy(() -> NetWorthStatement.createFrom(statement, NetWorthStatementId.generate(), Year.of(2026)))
                .isInstanceOf(DomainValidationException.class);
        NetWorthStatement foreign = NetWorthStatement.create(NetWorthStatementId.generate(),
                OwnerId.of(UUID.randomUUID()), Year.of(2025));
        assertThatThrownBy(() -> statement.overwriteWith(foreign)).isInstanceOf(IllegalArgumentException.class);
    }

    private static NetWorthEntryName name(String value) {
        return NetWorthEntryName.of(value);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
