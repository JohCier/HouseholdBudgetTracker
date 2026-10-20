package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemId;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.ItemAmount;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewCategoryItems;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.overview.OverviewItemGroup;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverviewEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
        BudgetItemId income = plan.addIncome(description("Gehalt"), yearly("2000.00"), null, null);
        BudgetItemId expense = plan.addExpense(description("Miete"), yearly("850.50"), ExpenseCategory.LEBENSUNTERHALT,
                null,
                new BudgetItemNote("Warmmiete"));
        repository.add(plan);
        HouseholdPlan otherPlan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2027));
        otherPlan.addIncome(description("Anderes Jahr"), yearly("1.00"), null, null);
        repository.add(otherPlan);

        HouseholdPlanOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).orElseThrow();

        assertThat(overview.planId()).isEqualTo(plan.id());
        assertThat(overview.year()).isEqualTo(Year.of(2026));
        assertThat(overview.items()).containsExactly(
                new OverviewItem(expense, description("Miete"), yearly("850.50"), ItemType.EXPENSE,
                        ExpenseCategory.LEBENSUNTERHALT, null, new BudgetItemNote("Warmmiete")),
                new OverviewItem(income, description("Gehalt"), yearly("2000.00"), ItemType.INCOME, null, null, null));
        assertThat(overview.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("850.50"));
    }

    /** MJ-2, MJ-3, MJ-9 */
    @Test
    void readsThePeriodOfEachItemAndTakesItemsWithoutOneAsYearly() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        BudgetItemId salary = plan.addIncome(description("Gehalt"), ItemAmount.monthly(money("3000.00")), null, null);
        repository.add(plan);
        UUID older = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO budget_item (id, household_plan_id, description, amount, type, category, position)
                        VALUES (:id, :planId, 'Versicherung', 1000.00, 'EXPENSE', 'LEBENSUNTERHALT', -1)
                        """)
                .param("id", older)
                .param("planId", plan.id().value())
                .update();

        HouseholdPlanOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).orElseThrow();

        assertThat(overview.items()).extracting(OverviewItem::id, OverviewItem::amount).containsExactly(
                tuple(salary, ItemAmount.monthly(money("3000.00"))),
                tuple(BudgetItemId.of(older), yearly("1000.00")));
        assertThat(overview.totalIncome()).isEqualTo(money("36000.00"));
        assertThat(overview.monthlyTotalExpense()).isEqualTo(money("83.33"));
    }

    /** UK-11, UK-12, UK-14, UK-15 */
    @Test
    void loadsTheItemsGroupedInTheStoredOrder() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        BudgetItemId maintenance = plan.addExpense(description("Wartung"), yearly("400.00"),
                ExpenseCategory.LEBENSUNTERHALT, BudgetItemSubcategory.of("Auto"), null);
        BudgetItemId fuel = plan.addExpense(description("Sprit"), yearly("1200.00"), ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.of("auto"), null);
        plan.addExpense(description("Kaltmiete"), yearly("9600.00"), ExpenseCategory.LEBENSUNTERHALT,
                BudgetItemSubcategory.of("Miete"), null);
        plan.addIncome(description("Gehalt"), yearly("3000.00"), null, null);
        plan.reorderItems(List.of(maintenance, fuel));
        plan.reorderSubcategories(ItemSection.expenses(ExpenseCategory.LEBENSUNTERHALT),
                List.of(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto")));
        repository.add(plan);

        HouseholdPlanOverview overview = queryHandler.findByOwnerAndYear(OWNER, Year.of(2026)).orElseThrow();

        assertThat(overview.incomeGroups()).extracting(OverviewItemGroup::subcategory)
                .containsExactly(BudgetItemSubcategory.OTHER);
        OverviewCategoryItems living = overview.expenseCategories().getFirst();
        assertThat(living.total()).isEqualTo(money("11200.00"));
        assertThat(living.groups()).extracting(OverviewItemGroup::subcategory)
                .containsExactly(BudgetItemSubcategory.of("Miete"), BudgetItemSubcategory.of("Auto"));
        assertThat(living.groups().getLast().items()).extracting(OverviewItem::id).containsExactly(maintenance, fuel);
        assertThat(living.groups().getLast().total()).isEqualTo(money("1600.00"));
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

    @Test
    void yearlyOverviewSumsTheItemsOfEveryPlanOfTheOwner() { // JU-1, JU-2, JU-3, JU-5
        HouseholdPlan previous = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2025));
        previous.addIncome(description("Gehalt"), money("20000.00"), null, null);
        previous.addIncome(description("Bonus"), money("10000.00"), null, null);
        previous.addExpense(description("Miete"), money("20000.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        previous.addExpense(description("Urlaub"), money("5000.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);
        repository.add(previous);
        HouseholdPlan current = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        current.addIncome(description("Gehalt"), money("30900.00"), null, null);
        current.addExpense(description("Miete"), money("24250.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        repository.add(current);
        HouseholdPlan empty = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2027));
        repository.add(empty);

        YearlyOverview overview = queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200));

        assertThat(overview.years()).extracting(YearlyOverviewEntry::planId)
                .containsExactly(empty.id(), current.id(), previous.id());
        YearlyOverviewEntry emptyYear = overview.years().get(0);
        assertThat(emptyYear.totalIncome()).isEqualTo(Money.ZERO);
        assertThat(emptyYear.totalExpense()).isEqualTo(Money.ZERO);
        assertThat(emptyYear.incomeChange().value()).isEqualByComparingTo("-100.00");
        YearlyOverviewEntry currentYear = overview.years().get(1);
        assertThat(currentYear.totalIncome()).isEqualTo(money("30900.00"));
        assertThat(currentYear.totalExpense()).isEqualTo(money("24250.00"));
        assertThat(currentYear.incomeChange().value()).isEqualByComparingTo("3.00");
        assertThat(currentYear.expenseChange().value()).isEqualByComparingTo("-3.00");
        assertThat(currentYear.balanceChange().value()).isEqualByComparingTo("33.00");
        assertThat(overview.years().get(2).totalExpense()).isEqualTo(money("25000.00"));
    }

    @Test
    void yearlyOverviewLeavesOutPlansOfOtherOwnersAndYearsOutsideTheRange() { // JU-1, HP-1
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()), Year.of(2026)));
        HouseholdPlan inRange = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        repository.add(inRange);
        jdbcClient.sql("INSERT INTO household_plan (id, owner_id, year) VALUES (:id, :owner, 2201)")
                .param("id", UUID.randomUUID())
                .param("owner", OWNER.value())
                .update();

        assertThat(queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200)).years())
                .extracting(YearlyOverviewEntry::planId)
                .containsExactly(inRange.id());
    }

    @Test
    void ownerWithoutPlansHasAnEmptyYearlyOverview() { // JU-13
        assertThat(queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200)).years()).isEmpty();
    }

    private static BudgetItemDescription description(String value) {
        return BudgetItemDescription.of(value);
    }

    private static ItemAmount yearly(String value) {
        return ItemAmount.yearly(money(value));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
