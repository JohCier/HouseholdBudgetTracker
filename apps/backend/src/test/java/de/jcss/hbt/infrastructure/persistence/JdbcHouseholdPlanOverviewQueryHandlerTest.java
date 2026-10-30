package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.IntegrationTest;
import de.jcss.hbt.domain.householdplan.HouseholdPlan;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverview;
import de.jcss.hbt.domain.householdplan.overview.OverviewCategoryItems;
import de.jcss.hbt.domain.householdplan.overview.OverviewItem;
import de.jcss.hbt.domain.householdplan.overview.OverviewItemGroup;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlanEntry;
import de.jcss.hbt.domain.householdplan.overview.DeletedPlans;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverview;
import de.jcss.hbt.domain.householdplan.overview.YearlyOverviewEntry;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemId;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.valueobject.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.valueobject.AmountPeriod;
import de.jcss.hbt.domain.householdplan.valueobject.ItemAmount;
import de.jcss.hbt.domain.householdplan.valueobject.ItemSection;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
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
        jdbcClient.sql("DELETE FROM one_time_item").update();
    }

    @Test
    void loadsThePlanOfTheYearWithItsItemsMostRecentlyAddedLast() {
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
                new OverviewItem(income, description("Gehalt"), yearly("2000.00"), ItemType.INCOME, null, null, null),
                new OverviewItem(expense, description("Miete"), yearly("850.50"), ItemType.EXPENSE,
                        ExpenseCategory.LEBENSUNTERHALT, null, new BudgetItemNote("Warmmiete")));
        assertThat(overview.totalIncome()).isEqualTo(money("2000.00"));
        assertThat(overview.totalExpense()).isEqualTo(money("850.50"));
    }

    /** PM-1, PM-2, PM-13; items from before the marks have none */
    @Test
    void readsTheMarksOfTheItemsAndSumsTheSavingPotential() {
        HouseholdPlan plan = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        BudgetItemId streaming = plan.addExpense(description("Streaming"), ItemAmount.monthly(money("15.00")),
                ExpenseCategory.FREIZEIT_SPASS, null, null);
        BudgetItemId gym = plan.addExpense(description("Fitnessstudio"), yearly("360.00"),
                ExpenseCategory.FREIZEIT_SPASS, null, null);
        BudgetItemId salary = plan.addIncome(description("Gehalt"), ItemAmount.monthly(money("3000.00")), null, null);
        plan.markImportant(salary, true);
        plan.changeSavingPotential(streaming, SavingPotential.monthly(money("5.00")));
        plan.changeSavingPotential(gym, SavingPotential.monthly(money("30.00")));
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

        assertThat(overview.items())
                .extracting(OverviewItem::id, OverviewItem::important, OverviewItem::savingPotential)
                .containsExactly(
                        tuple(streaming, false, SavingPotential.monthly(money("5.00"))),
                        tuple(gym, false, SavingPotential.monthly(money("30.00"))),
                        tuple(salary, true, null),
                        tuple(BudgetItemId.of(older), false, null));
        assertThat(overview.monthlySavingPotential()).isEqualTo(money("35.00"));
        assertThat(overview.savingPotential()).isEqualTo(money("420.00"));
        assertThat(overview.balance()).isEqualTo(money("34460.00"));
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
    void yearlyOverviewSumsTheItemsOfEveryPlanOfTheOwner() { // JU-1, JU-2, JU-3, JU-5, MJ-8
        HouseholdPlan previous = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2025));
        previous.addIncome(description("Gehalt"), yearly("20000.00"), null, null);
        previous.addIncome(description("Bonus"), yearly("10000.00"), null, null);
        previous.addExpense(description("Miete"), yearly("20000.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
        previous.addExpense(description("Urlaub"), yearly("5000.00"), ExpenseCategory.FREIZEIT_SPASS, null, null);
        repository.add(previous);
        HouseholdPlan current = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        // A monthly amount counts twelve times (MJ-8)
        current.addIncome(description("Gehalt"), ItemAmount.monthly(money("2575.00")), null, null);
        current.addExpense(description("Miete"), yearly("24250.00"), ExpenseCategory.LEBENSUNTERHALT, null, null);
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
    void deletedPlansAreLeftOutOfTheOverviewsButCountedAndListed() { // GP-3, GP-21, GP-22, GP-24
        HouseholdPlan earlier = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        earlier.addIncome(description("Gehalt"), yearly("38400.00"), null, null);
        earlier.addExpense(description("Miete"), new ItemAmount(Money.of(new BigDecimal("900.00")), AmountPeriod.MONTHLY),
                ExpenseCategory.LEBENSUNTERHALT, null, null);
        repository.add(earlier);
        markDeleted(earlier, "2026-10-20T18:42:00Z");
        HouseholdPlan later = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026));
        repository.add(later);
        markDeleted(later, "2026-10-29T20:15:00Z");
        HouseholdPlan of2023 = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2023));
        repository.add(of2023);
        markDeleted(of2023, "2026-11-02T09:05:00Z");
        HouseholdPlan active = HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2027));
        repository.add(active);
        HouseholdPlan foreign = HouseholdPlan.create(HouseholdPlanId.generate(), OwnerId.of(UUID.randomUUID()),
                Year.of(2026));
        repository.add(foreign);
        repository.markDeleted(foreign);

        YearlyOverview overview = queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200));
        DeletedPlans deleted = queryHandler.findDeletedPlansByOwner(OWNER, Year.of(1900), Year.of(2200));

        assertThat(queryHandler.findByOwnerAndYear(OWNER, Year.of(2026))).isEmpty();
        assertThat(overview.years()).extracting(YearlyOverviewEntry::planId).containsExactly(active.id());
        assertThat(overview.years().getFirst().incomeChange()).isNull();
        assertThat(overview.deletedPlanCount()).isEqualTo(3);
        assertThat(deleted.plans()).extracting(DeletedPlanEntry::planId)
                .containsExactly(later.id(), earlier.id(), of2023.id());
        DeletedPlanEntry entry = deleted.plans().get(1);
        assertThat(entry.deletedAt()).isEqualTo(Instant.parse("2026-10-20T18:42:00Z"));
        assertThat(entry.itemCount()).isEqualTo(2);
        assertThat(entry.totalIncome()).isEqualTo(Money.of(new BigDecimal("38400.00")));
        assertThat(entry.totalExpense()).isEqualTo(Money.of(new BigDecimal("10800.00")));
        assertThat(entry.monthlyBalance()).isEqualTo(Money.of(new BigDecimal("2300.00")));
        assertThat(deleted.plans().getFirst().itemCount()).isZero();
    }

    private void markDeleted(HouseholdPlan plan, String deletedAt) {
        jdbcClient.sql("UPDATE household_plan SET deleted_at = :deletedAt WHERE id = :id")
                .param("deletedAt", Timestamp.from(Instant.parse(deletedAt)))
                .param("id", plan.id().value())
                .update();
    }

    @Test
    void ownerWithoutPlansHasAnEmptyYearlyOverview() { // JU-13
        assertThat(queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200)).years()).isEmpty();
    }

    // docs/specs/einmalposten.md
    @Test
    void yearlyOverviewSumsTheOneTimeItemsPerYearAlsoWithoutPlan() { // EP-12, EP-13
        repository.add(HouseholdPlan.create(HouseholdPlanId.generate(), OWNER, Year.of(2026)));
        oneTimeItem(OWNER, "EXPENSE", "1200.00", "2026-03-15");
        oneTimeItem(OWNER, "INCOME", "800.00", "2026-07-10");
        oneTimeItem(OWNER, "INCOME", "2000.00", "2027-12-01");
        oneTimeItem(OwnerId.of(UUID.randomUUID()), "INCOME", "5.00", "2026-01-01");

        List<YearlyOverviewEntry> years = queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(1900), Year.of(2200))
                .years();

        assertThat(years).extracting(YearlyOverviewEntry::year, YearlyOverviewEntry::hasPlan,
                        YearlyOverviewEntry::oneTimeIncome, YearlyOverviewEntry::oneTimeExpense)
                .containsExactly(
                        tuple(Year.of(2027), false, money("2000.00"), Money.ZERO),
                        tuple(Year.of(2026), true, money("800.00"), money("1200.00")));
    }

    @Test
    void yearlyOverviewLeavesOutOneTimeItemsOutsideTheRange() { // EP-3, EP-12
        oneTimeItem(OWNER, "INCOME", "1.00", "2025-12-31");
        oneTimeItem(OWNER, "INCOME", "2.00", "2026-01-01");

        assertThat(queryHandler.findYearlyOverviewByOwner(OWNER, Year.of(2026), Year.of(2026)).years())
                .extracting(YearlyOverviewEntry::oneTimeIncome)
                .containsExactly(money("2.00"));
    }

    private void oneTimeItem(OwnerId owner, String type, String amount, String date) {
        jdbcClient.sql("""
                        INSERT INTO one_time_item (id, owner_id, type, description, amount, item_date, item_rank)
                        VALUES (:id, :owner, :type, 'Einmalig', CAST(:amount AS NUMERIC), CAST(:date AS DATE), 1)
                        """)
                .param("id", UUID.randomUUID())
                .param("owner", owner.value())
                .param("type", type)
                .param("amount", amount)
                .param("date", date)
                .update();
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
