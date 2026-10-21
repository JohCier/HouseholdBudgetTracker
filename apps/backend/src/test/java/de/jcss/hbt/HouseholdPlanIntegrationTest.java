package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plans end to end: overview, sessions, ownership, one plan per year, copying and deleting.
 */
@IntegrationTest
class HouseholdPlanIntegrationTest extends HouseholdPlanIntegrationTestSupport {

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
}
