package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The repository is only used to arrange test data; the query handler itself never touches it. */
@IntegrationTest
class JdbcHouseholdPlanOverviewQueryHandlerTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());

    @Autowired
    private JdbcHouseholdPlanOverviewQueryHandler queryHandler;

    @Autowired
    private HouseholdPlanRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM household_plan").update();
    }

    @Test
    void loadsThePlanOfTheYearWithItsItemsMostRecentlyAddedFirst() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        BudgetItemId income = plan.addIncome(description("Gehalt"), money("2000.00"), null);
        BudgetItemId expense = plan.addExpense(description("Miete"), money("850.50"), ExpenseCategory.LEBENSUNTERHALT,
                new BudgetItemNote("Warmmiete"));
        repository.add(plan);
        HouseholdPlan otherPlan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2027));
        otherPlan.addIncome(description("Anderes Jahr"), money("1.00"), null);
        repository.add(otherPlan);

        HouseholdPlanOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).orElseThrow();

        assertThat(overview.planId()).isEqualTo(plan.id());
        assertThat(overview.year()).isEqualTo(Year.of(2026));
        assertThat(overview.items()).containsExactly(
                new OverviewItem(expense, description("Miete"), money("850.50"), ItemType.EXPENSE,
                        ExpenseCategory.LEBENSUNTERHALT, new BudgetItemNote("Warmmiete")),
                new OverviewItem(income, description("Gehalt"), money("2000.00"), ItemType.INCOME, null, null));
        assertThat(overview.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("850.50"));
    }

    @Test
    void emptyPlanHasNoItemsAndAZeroOverview() {
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026)));

        HouseholdPlanOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).orElseThrow();

        assertThat(overview.items()).isEmpty();
        assertThat(overview.totalIncome().isZero()).isTrue();
        assertThat(overview.totalExpense().isZero()).isTrue();
    }

    @Test
    void yearWithoutPlanHasNoOverview() {
        assertThat(queryHandler.findByOwnerAndYear(OWNER, Year.of(2026))).isEmpty();
    }

    @Test
    void plansOfOtherOwnersAreNotIncluded() {
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()), Year.of(2026)));

        assertThat(queryHandler.findByOwnerAndYear(OWNER, Year.of(2026))).isEmpty();
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
