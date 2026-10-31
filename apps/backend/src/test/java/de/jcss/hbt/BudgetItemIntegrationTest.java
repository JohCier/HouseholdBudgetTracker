package de.jcss.hbt;

import de.jcss.hbt.test.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
 * Budget items end to end: changing, marking and removing them, the overview in the answer and the audit log.
 */
@IntegrationTest
class BudgetItemIntegrationTest extends HouseholdPlanIntegrationTestSupport {

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

    /** PM-2, PM-2a, PM-4, PM-5, PM-7, PM-13 */
    @Test
    void marksAreStoredShownInTheOverviewAndAudited() throws Exception {
        String planId = createPlan(2026);
        String salaryId = JsonPath.read(post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"3000.00","period":"MONTHLY"}"""), "$.id");
        String streamingId = JsonPath.read(post("/api/plans/" + planId + "/expenses", """
                {"description":"Streaming","amount":"15.00","period":"MONTHLY","category":"FREIZEIT_SPASS"}"""), "$.id");

        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/important", planId, streamingId).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"important":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].important").value(true));
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", planId, salaryId).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monthlyAmount":"5.00"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", planId, streamingId).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monthlyAmount":"15.01"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/plans/{planId}/items/{itemId}/saving-potential", planId, streamingId).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monthlyAmount":"5.00"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].savingPotential").value("5.00"))
                .andExpect(jsonPath("$.monthlySavingPotential").value("5.00"))
                .andExpect(jsonPath("$.savingPotential").value("60.00"))
                .andExpect(jsonPath("$.monthlyBalance").value("2985.00"));
        changeItem(planId, streamingId, """
                {"description":"Streaming","amount":"36.00","period":"YEARLY","category":"FREIZEIT_SPASS"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].important").value(true))
                .andExpect(jsonPath("$.items[1].savingPotential").value("3.00"));

        assertThat(jdbcClient.sql("""
                        SELECT payload ->> 'mark' || ':' || (payload ->> 'marked') FROM audit_log
                        WHERE event_type = 'BudgetItemMarked' ORDER BY id""")
                .query(String.class).list())
                .containsExactly("IMPORTANT:true", "SAVING_POTENTIAL:true");
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
}
