package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import de.jcss.hbt.infrastructure.security.AuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full stack end to end: HTTP request in through the web layer, the write
 * side (application service, aggregate, repository) and the read side (query handler),
 * persisted in PostgreSQL, including the event listener running after the commit.
 * <p>
 * Requests carry a session cookie like the one the users app sets at login, signed here
 * with the same secret.
 */
@IntegrationTest
class HouseholdPlanIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private AuthProperties auth;

    private MockMvc mockMvc;
    private Cookie alice;
    private Cookie bob;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        alice = session(UUID.randomUUID(), auth.issuer(), Instant.now().plus(Duration.ofHours(1)));
        bob = session(UUID.randomUUID(), auth.issuer(), Instant.now().plus(Duration.ofHours(1)));
        jdbcClient.sql("DELETE FROM household_plan").update();
        jdbcClient.sql("DELETE FROM audit_log").update();
    }

    @Test
    void tracksIncomeAndExpensesPerPlanAndComputesOverview() throws Exception {
        String plan2026 = createPlan(2026);
        String plan2027 = createPlan(2027);
        post("/api/plans/" + plan2026 + "/incomes", """
                {"description":"Gehalt","amount":"2000.00","period":"YEARLY"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Miete","amount":"1000.00","period":"YEARLY","category":"LEBENSUNTERHALT"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Konzert","amount":"300.00","period":"YEARLY","category":"FREIZEIT_SPASS"}""");
        post("/api/plans/" + plan2027 + "/expenses", """
                {"description":"Urlaub","amount":"900.00","period":"YEARLY","category":"FREIZEIT_SPASS"}""");

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan2026))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].description").value("Gehalt"))
                .andExpect(jsonPath("$.totalIncome").value("2000.00"))
                .andExpect(jsonPath("$.totalExpense").value("1300.00"))
                .andExpect(jsonPath("$.balance").value("700.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false));

        mockMvc.perform(get("/api/plans").param("year", "2027").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan2027))
                .andExpect(jsonPath("$.totalExpense").value("900.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(true));
    }

    @Test
    void yearWithoutPlanIsNotFound() throws Exception {
        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void plansAreOnlyVisibleToAndChangeableByTheirOwner() throws Exception {
        String alicesPlan = createPlan(2026);
        String alicesItem = JsonPath.read(post("/api/plans/" + alicesPlan + "/incomes", """
                {"description":"Gehalt","amount":"1500.00","period":"YEARLY"}"""), "$.id");

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/incomes", alicesPlan)
                        .cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Fremd","amount":"1.00","period":"YEARLY"}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", alicesPlan, alicesItem)
                        .cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Fremd","amount":"1.00","period":"YEARLY"}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", alicesPlan, alicesItem).cookie(bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(alicesItem))
                .andExpect(jsonPath("$.items[0].amount").value("1500.00"));
    }

    @Test
    void everyUserHasTheirOwnPlanPerYear() throws Exception {
        String alicesPlan = createPlan(2026);

        String bobsPlan = JsonPath.read(mockMvc.perform(MockMvcRequestBuilders.post("/api/plans")
                        .cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        assertThat(bobsPlan).isNotEqualTo(alicesPlan);
        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(bob))
                .andExpect(jsonPath("$.id").value(bobsPlan));
    }

    @Test
    void apiDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/api/docs")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void requestsWithoutValidSessionAreUnauthorized() throws Exception {
        UUID user = UUID.randomUUID();
        Cookie expired = session(user, auth.issuer(), Instant.now().minus(Duration.ofMinutes(5)));
        Cookie foreignIssuer = session(user, "someone-else", Instant.now().plus(Duration.ofHours(1)));
        Cookie tampered = new Cookie(auth.cookieName(), alice.getValue().substring(0, alice.getValue().length() - 2));

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isUnauthorized());
        for (Cookie invalid : new Cookie[]{expired, foreignIssuer, tampered}) {
            mockMvc.perform(get("/api/plans").param("year", "2026").cookie(invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void secondPlanForTheSameYearIsAConflict() throws Exception {
        createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans")
                        .cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void removingAnItemUpdatesTheItemsAndIsAudited() throws Exception {
        String planId = createPlan(2026);
        String itemId = JsonPath.read(post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"1500.00","period":"YEARLY"}"""), "$.id");

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, itemId).cookie(alice))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        assertThat(jdbcClient.sql("""
                        SELECT event_type, household_plan_id, payload ->> 'itemId' AS item_id
                        FROM audit_log WHERE event_type = 'BudgetItemRemoved'""")
                .query().listOfRows())
                .containsExactly(Map.of(
                        "event_type", "BudgetItemRemoved",
                        "household_plan_id", UUID.fromString(planId),
                        "item_id", itemId));
        assertThat(jdbcClient.sql("SELECT bool_and(occurred_at <= recorded_at) FROM audit_log").query(Boolean.class).single())
                .as("changes happened before they were logged")
                .isTrue();
    }

    @Test
    void everyChangeIsAuditedWithTheFullEvent() throws Exception {
        String planId = createPlan(2026);
        String incomeId = JsonPath.read(post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"1500.00","period":"YEARLY"}"""), "$.id");
        String expenseId = JsonPath.read(post("/api/plans/" + planId + "/expenses", """
                {"description":"Miete","amount":"700.00","period":"YEARLY","category":"LEBENSUNTERHALT"}"""), "$.id");

        assertThat(jdbcClient.sql("""
                        SELECT event_type, household_plan_id, payload ->> 'planId' AS payload_plan_id,
                               payload ->> 'itemId' AS item_id
                        FROM audit_log ORDER BY id""")
                .query().listOfRows())
                .extracting(row -> row.get("event_type"), row -> row.get("household_plan_id"),
                        row -> row.get("payload_plan_id"), row -> row.get("item_id"))
                .containsExactly(
                        tuple("HouseholdPlanCreated", UUID.fromString(planId), planId, null),
                        tuple("IncomeAdded", UUID.fromString(planId), planId, incomeId),
                        tuple("ExpenseAdded", UUID.fromString(planId), planId, expenseId));
        assertThat(jdbcClient.sql("""
                        SELECT (payload ->> 'year')::int FROM audit_log WHERE event_type = 'HouseholdPlanCreated'""")
                .query(Integer.class).single())
                .isEqualTo(2026);
    }

    /** PB-1, PB-4, PB-5, PB-7 */
    @Test
    void changingAnItemKeepsItsPlaceUpdatesTheOverviewAndIsAudited() throws Exception {
        String planId = createPlan(2026);
        post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"2000.00","period":"YEARLY"}""");
        String rentId = JsonPath.read(post("/api/plans/" + planId + "/expenses", """
                {"description":"Miete","amount":"850.00","period":"YEARLY","category":"LEBENSUNTERHALT"}"""), "$.id");
        post("/api/plans/" + planId + "/expenses", """
                {"description":"Konzert","amount":"100.00","period":"YEARLY","category":"FREIZEIT_SPASS"}""");

        changeItem(planId, rentId, """
                {"description":"Miete","amount":"900.00","period":"YEARLY","category":"LEBENSUNTERHALT"}""")
                .andExpect(status().isOk());
        changeItem(planId, rentId, """
                {"description":"Miete","amount":"900","period":"YEARLY","category":"LEBENSUNTERHALT"}""")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[1].id").value(rentId))
                .andExpect(jsonPath("$.items[1].amount").value("900.00"))
                .andExpect(jsonPath("$.totalExpense").value("1000.00"))
                .andExpect(jsonPath("$.balance").value("1000.00"));
        assertThat(jdbcClient.sql("""
                        SELECT payload ->> 'itemId' FROM audit_log WHERE event_type = 'BudgetItemChanged'""")
                .query(String.class).list())
                .as("only the change that changed something is audited")
                .containsExactly(rentId);
    }

    /** PB-2, PB-3, PB-6 */
    @Test
    void invalidChangesAreRejectedAndLeaveTheItemAsItWas() throws Exception {
        String planId = createPlan(2026);
        String incomeId = JsonPath.read(post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"2000.00","period":"YEARLY"}"""), "$.id");
        String expenseId = JsonPath.read(post("/api/plans/" + planId + "/expenses", """
                {"description":"Miete","amount":"850.00","period":"YEARLY","category":"LEBENSUNTERHALT"}"""), "$.id");

        changeItem(planId, incomeId, """
                {"description":"Gehalt","amount":"2000.00","period":"YEARLY","category":"LEBENSUNTERHALT"}""")
                .andExpect(status().isBadRequest());
        changeItem(planId, expenseId, """
                {"description":"Miete","amount":"850.00","period":"YEARLY"}""")
                .andExpect(status().isBadRequest());
        changeItem(planId, expenseId, """
                {"description":"Miete","amount":"0","period":"YEARLY"}""")
                .andExpect(status().isBadRequest());
        changeItem(planId, UUID.randomUUID().toString(), """
                {"description":"Miete","amount":"850.00","period":"YEARLY"}""")
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(jsonPath("$.totalIncome").value("2000.00"))
                .andExpect(jsonPath("$.totalExpense").value("850.00"));
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM audit_log WHERE event_type = 'BudgetItemChanged'")
                .query(Long.class).single()).isZero();
    }

    /** AU-1, AU-2, AU-3, AU-4 */
    @Test
    void everyChangeAnswersWithTheOverviewAfterIt() throws Exception {
        String created = post("/api/plans", """
                {"year":2026}""");
        String planId = JsonPath.read(created, "$.id");
        assertThat(JsonPath.<String>read(created, "$.overview.id")).isEqualTo(planId);
        assertThat(JsonPath.<List<?>>read(created, "$.overview.items")).isEmpty();

        String added = post("/api/plans/" + planId + "/expenses", """
                {"description":"Miete","amount":"850.00","period":"YEARLY","category":"LEBENSUNTERHALT"}""");
        String rentId = JsonPath.read(added, "$.id");
        assertThat(JsonPath.<String>read(added, "$.overview.items[0].id")).isEqualTo(rentId);
        assertThat(JsonPath.<String>read(added, "$.overview.totalExpense")).isEqualTo("850.00");
        assertThat(JsonPath.<Object>read(added, "$.overview")).isEqualTo(JsonPath.read(overview(2026), "$"));

        String changed = changeItem(planId, rentId, """
                {"description":"Miete","amount":"900.00","period":"YEARLY","category":"LEBENSUNTERHALT"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value("900.00"))
                .andReturn().getResponse().getContentAsString();
        assertThat(changed).isEqualTo(overview(2026));

        String removed = mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, rentId).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(removed).isEqualTo(overview(2026));
    }

    @Test
    void failedRemovalIsNotAudited() throws Exception {
        String planId = createPlan(2026);

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, UUID.randomUUID()).cookie(alice))
                .andExpect(status().isNotFound());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM audit_log WHERE event_type = 'BudgetItemRemoved'")
                .query(Long.class).single()).isZero();
    }

    @Test
    void rejectsFractionsOfCents() throws Exception {
        String planId = createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/incomes", planId)
                        .cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"10.005","period":"YEARLY"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    /** PU-3, PU-4, PU-5, PU-8, PU-11, MJ-1 */
    @Test
    void copyingAPlanCreatesTheTargetYearWithAllItemsAndIsAudited() throws Exception {
        String source = createPlan(2026);
        post("/api/plans/" + source + "/incomes", """
                {"description":"Gehalt","amount":"3000.00","period":"YEARLY"}""");
        post("/api/plans/" + source + "/expenses", """
                {"description":"Wartung","amount":"400.00","period":"YEARLY","category":"LEBENSUNTERHALT","subcategory":"Auto","note":"Inspektion im Mai"}""");
        post("/api/plans/" + source + "/expenses", """
                {"description":"Miete","amount":"900.00","period":"MONTHLY","category":"LEBENSUNTERHALT","subcategory":"Wohnen"}""");
        mockMvc.perform(put("/api/plans/{planId}/subcategory-order", source).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","category":"LEBENSUNTERHALT","subcategories":["Auto","Wohnen"]}"""))
                .andExpect(status().isOk());
        jdbcClient.sql("DELETE FROM audit_log").update();

        String target = JsonPath.read(post("/api/plans/" + source + "/copies", """
                {"targetYear":2027}"""), "$.id");

        String copied = overview(2027);
        String original = overview(2026);
        assertThat(JsonPath.<String>read(copied, "$.id")).isEqualTo(target).isNotEqualTo(source);
        for (String path : List.of("$.items[*].description", "$.items[*].amount", "$.items[*].period",
                "$.items[*].subcategory", "$.items[*].note", "$.expenseCategories..subcategory",
                "$.categoryBreakdowns", "$.totalIncome", "$.balance")) {
            assertThat(JsonPath.<Object>read(copied, path)).as(path).isEqualTo(JsonPath.read(original, path));
        }
        assertThat(JsonPath.<List<String>>read(copied, "$.items[*].id"))
                .doesNotContainAnyElementsOf(JsonPath.read(original, "$.items[*].id"));
        assertThat(jdbcClient.sql("""
                        SELECT event_type, household_plan_id, payload ->> 'sourcePlanId' AS source,
                               payload ->> 'overwritten' AS overwritten
                        FROM audit_log ORDER BY id""")
                .query().listOfRows())
                .extracting(row -> row.get("event_type"), row -> row.get("household_plan_id"),
                        row -> row.get("source"), row -> row.get("overwritten"))
                .containsExactly(
                        tuple("HouseholdPlanCreated", UUID.fromString(target), null, null),
                        tuple("HouseholdPlanCopied", UUID.fromString(target), source, "false"));
    }

    /** PU-9, PU-11 */
    @Test
    void copyingOverAnExistingPlanNeedsConsentAndReplacesItsItems() throws Exception {
        String source = createPlan(2026);
        post("/api/plans/" + source + "/incomes", """
                {"description":"Gehalt","amount":"3000.00","period":"YEARLY"}""");
        String target = createPlan(2027);
        post("/api/plans/" + target + "/incomes", """
                {"description":"Bonus","amount":"500.00","period":"YEARLY"}""");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/copies", source).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}"""))
                .andExpect(status().isConflict());
        assertThat(JsonPath.<List<String>>read(overview(2027), "$.items[*].description")).containsExactly("Bonus");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/copies", source).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027,"overwrite":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target))
                .andExpect(jsonPath("$.overview.items.length()").value(1))
                .andExpect(jsonPath("$.overview.items[0].description").value("Gehalt"));

        assertThat(JsonPath.<List<String>>read(overview(2027), "$.items[*].description")).containsExactly("Gehalt");
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM audit_log
                        WHERE event_type = 'HouseholdPlanCopied' AND (payload ->> 'overwritten')::boolean""")
                .query(Integer.class).single())
                .isEqualTo(1);
    }

    /** HP-4 */
    @Test
    void plansOfOthersCannotBeCopied() throws Exception {
        String source = createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/copies", source).cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/plans").param("year", "2027").cookie(bob))
                .andExpect(status().isNotFound());
    }

    /** PL-1 to PL-3, PL-6 */
    @Test
    void deletingAPlanRemovesItWithAllItemsAndIsAudited() throws Exception {
        String deleted = createPlan(2026);
        post("/api/plans/" + deleted + "/incomes", """
                {"description":"Gehalt","amount":"3000.00","period":"YEARLY"}""");
        post("/api/plans/" + deleted + "/expenses", """
                {"description":"Miete","amount":"900.00","period":"MONTHLY","category":"LEBENSUNTERHALT"}""");
        String kept = createPlan(2027);
        post("/api/plans/" + kept + "/incomes", """
                {"description":"Bonus","amount":"500.00","period":"YEARLY"}""");
        jdbcClient.sql("DELETE FROM audit_log").update();

        mockMvc.perform(delete("/api/plans/{planId}", deleted).cookie(alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isNotFound());
        assertThat(JsonPath.<List<String>>read(overview(2027), "$.items[*].description")).containsExactly("Bonus");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM budget_item WHERE household_plan_id = :id")
                .param("id", UUID.fromString(deleted)).query(Integer.class).single()).isZero();
        assertThat(jdbcClient.sql("""
                        SELECT event_type, household_plan_id, (payload ->> 'year')::int AS year,
                               (payload ->> 'itemCount')::int AS item_count
                        FROM audit_log ORDER BY id""")
                .query().listOfRows())
                .extracting(row -> row.get("event_type"), row -> row.get("household_plan_id"),
                        row -> row.get("year"), row -> row.get("item_count"))
                .containsExactly(tuple("HouseholdPlanDeleted", UUID.fromString(deleted), 2026, 2));

        mockMvc.perform(delete("/api/plans/{planId}", deleted).cookie(alice))
                .andExpect(status().isNotFound());
        String recreated = createPlan(2026);
        assertThat(JsonPath.<List<Object>>read(overview(2026), "$.items")).isEmpty();
        assertThat(recreated).isNotEqualTo(deleted);
    }

    /** PL-1, HP-4 */
    @Test
    void plansOfOthersCannotBeDeleted() throws Exception {
        String plan = createPlan(2026);

        mockMvc.perform(delete("/api/plans/{planId}", plan).cookie(bob))
                .andExpect(status().isNotFound());

        assertThat(JsonPath.<String>read(overview(2026), "$.id")).isEqualTo(plan);
    }

    private ResultActions changeItem(String planId, String itemId, String json)
            throws Exception {
        return mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", planId, itemId)
                .cookie(alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String overview(int year) throws Exception {
        return mockMvc.perform(get("/api/plans").param("year", String.valueOf(year)).cookie(alice))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String createPlan(int year) throws Exception {
        return JsonPath.read(post("/api/plans", """
                {"year":%d}""".formatted(year)), "$.id");
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** A session cookie as set by the users app, signed with the shared secret. */
    private Cookie session(UUID user, String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.toString())
                .issuer(issuer)
                .issuedAt(expiresAt.minus(Duration.ofHours(2)))
                .expiresAt(expiresAt)
                .build();
        String token = NimbusJwtEncoder.withSecretKey(auth.signingKey()).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Cookie(auth.cookieName(), token);
    }
}
