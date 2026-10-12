package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.TestcontainersConfiguration;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcHouseholdPlanRepositoryTest {

    private static final Year YEAR = Year.of(2026);

    @Autowired
    private JdbcHouseholdPlanRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM household_plan").update();
    }

    @Test
    void storesAndRestoresPlanWithAllItemAttributesInOrder() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), YEAR);
        plan.addIncome(description("Gehalt"), money("2500.00"));
        plan.addExpense(description("Miete"), money("850.50"), ExpenseCategory.LEBENSUNTERHALT);

        repository.save(plan);

        HouseholdPlan restored = repository.findById(plan.id()).orElseThrow();
        assertThat(restored.year()).isEqualTo(YEAR);
        assertThat(restored.items())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(plan.items());
    }

    @Test
    void savingAgainAddsNewAndDeletesRemovedItems() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), YEAR);
        BudgetItemId kept = plan.addIncome(description("Gehalt"), money("100.00"));
        BudgetItemId removed = plan.addIncome(description("Bonus"), money("50.00"));
        repository.save(plan);

        HouseholdPlan loaded = repository.findById(plan.id()).orElseThrow();
        loaded.removeItem(removed);
        BudgetItemId added =
                loaded.addExpense(description("ETF"), money("20.00"), ExpenseCategory.SPAREN_INVESTIEREN);
        repository.save(loaded);

        assertThat(repository.findById(plan.id()).orElseThrow().items())
                .extracting(BudgetItem::id)
                .containsExactly(kept, added);
    }

    @Test
    void removingTheLastItemLeavesAnEmptyPlan() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), YEAR);
        BudgetItemId itemId = plan.addIncome(description("Gehalt"), money("100.00"));
        repository.save(plan);

        plan.removeItem(itemId);
        repository.save(plan);

        assertThat(repository.findById(plan.id()).orElseThrow().items()).isEmpty();
    }

    @Test
    void doesNotFindUnknownPlan() {
        assertThat(repository.findById(HouseholdPlanId.generate())).isEmpty();
    }

    @Test
    void knowsWhetherAYearHasAPlan() {
        repository.save(HouseholdPlan.create(HouseholdPlanId.generate(), YEAR));

        assertThat(repository.existsForYear(YEAR)).isTrue();
        assertThat(repository.existsForYear(Year.of(2027))).isFalse();
    }

    @Test
    void rejectsSecondPlanForTheSameYear() {
        repository.save(HouseholdPlan.create(HouseholdPlanId.generate(), YEAR));

        assertThatThrownBy(() -> repository.save(HouseholdPlan.create(HouseholdPlanId.generate(), YEAR)))
                .isInstanceOf(HouseholdPlanAlreadyExistsException.class);
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
