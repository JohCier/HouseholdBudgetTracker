package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.TestcontainersConfiguration;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

/** The repository is only used to arrange test data; the query handler itself never touches it. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcHouseholdPlanOverviewQueryHandlerTest {

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
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), Year.of(2026));
        BudgetItemId income = plan.addIncome(description("Gehalt"), money("2000.00"));
        BudgetItemId expense = plan.addExpense(description("Miete"), money("850.50"), ExpenseCategory.LEBENSUNTERHALT);
        repository.save(plan);
        HouseholdPlan otherPlan = HouseholdPlan.create(HouseholdPlanId.generate(), Year.of(2027));
        otherPlan.addIncome(description("Anderes Jahr"), money("1.00"));
        repository.save(otherPlan);

        HouseholdPlanOverview overview = queryHandler.findByYear(Year.of(2026)).orElseThrow();

        assertThat(overview.planId()).isEqualTo(plan.id());
        assertThat(overview.year()).isEqualTo(Year.of(2026));
        assertThat(overview.items()).containsExactly(
                new OverviewItem(expense, description("Miete"), money("850.50"), ItemType.EXPENSE,
                        ExpenseCategory.LEBENSUNTERHALT),
                new OverviewItem(income, description("Gehalt"), money("2000.00"), ItemType.INCOME, null));
        assertThat(overview.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("850.50"));
    }

    @Test
    void emptyPlanHasNoItemsAndAZeroOverview() {
        repository.save(HouseholdPlan.create(HouseholdPlanId.generate(), Year.of(2026)));

        HouseholdPlanOverview overview = queryHandler.findByYear(Year.of(2026)).orElseThrow();

        assertThat(overview.items()).isEmpty();
        assertThat(overview.totalIncome().isZero()).isTrue();
        assertThat(overview.totalExpense().isZero()).isTrue();
    }

    @Test
    void yearWithoutPlanHasNoOverview() {
        assertThat(queryHandler.findByYear(Year.of(2026))).isEmpty();
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
