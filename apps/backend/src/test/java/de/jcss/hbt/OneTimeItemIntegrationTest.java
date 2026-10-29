package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One-time items end to end (docs/specs/einmalposten.md). */
@IntegrationTest
class OneTimeItemIntegrationTest extends HouseholdPlanIntegrationTestSupport {

    private static final String BACK_PAYMENT = """
            {"type":"EXPENSE","description":"Nebenkostennachzahlung","amount":"1200.00","date":"2026-03-15",
             "note":"Abrechnung 2025"}""";
    private static final String TAX_REFUND = """
            {"type":"INCOME","description":"Steuererstattung","amount":"800.00","date":"2026-07-10"}""";

    /** EP-1, EP-6, EP-8, EP-9 */
    @Test
    void oneTimeItemsAreListedPerYearAndLeaveThePlanUntouched() throws Exception {
        String plan = createPlan(2026);
        post("/api/plans/" + plan + "/incomes", """
                {"description":"Gehalt","amount":"3000.00","period":"MONTHLY"}""");
        post("/api/one-time-items", TAX_REFUND);
        post("/api/one-time-items", BACK_PAYMENT);

        mockMvc.perform(get("/api/one-time-items").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].description").value(
                        contains("Nebenkostennachzahlung", "Steuererstattung")))
                .andExpect(jsonPath("$.items[0].note").value("Abrechnung 2025"))
                .andExpect(jsonPath("$.totalIncome").value("800.00"))
                .andExpect(jsonPath("$.totalExpense").value("1200.00"))
                .andExpect(jsonPath("$.balance").value("-400.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(true));
        String overview = overview(2026);
        assertThat(JsonPath.<String>read(overview, "$.totalIncome")).isEqualTo("36000.00");
        assertThat(JsonPath.<String>read(overview, "$.totalExpense")).isEqualTo("0.00");
        assertThat(JsonPath.<List<?>>read(overview, "$.items")).hasSize(1);
    }

    /** EP-12, EP-13 */
    @Test
    void yearlyOverviewHasOneTimeTotalsAndYearsWithOnlyOneTimeItems() throws Exception {
        String plan = createPlan(2026);
        post("/api/one-time-items", BACK_PAYMENT);
        post("/api/one-time-items", TAX_REFUND);
        post("/api/one-time-items", """
                {"type":"INCOME","description":"Bonus","amount":"2000.00","date":"2027-12-01"}""");

        mockMvc.perform(get("/api/plans/years").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years.length()").value(2))
                .andExpect(jsonPath("$.years[0].year").value(2027))
                .andExpect(jsonPath("$.years[0].planId").isEmpty())
                .andExpect(jsonPath("$.years[0].totalIncome").isEmpty())
                .andExpect(jsonPath("$.years[0].oneTimeIncome").value("2000.00"))
                .andExpect(jsonPath("$.years[0].oneTimeBalance").value("2000.00"))
                .andExpect(jsonPath("$.years[1].planId").value(plan))
                .andExpect(jsonPath("$.years[1].totalIncome").value("0.00"))
                .andExpect(jsonPath("$.years[1].oneTimeIncome").value("800.00"))
                .andExpect(jsonPath("$.years[1].oneTimeExpense").value("1200.00"))
                .andExpect(jsonPath("$.years[1].oneTimeBalance").value("-400.00"));
    }

    /** EP-5 */
    @Test
    void deletingOrCopyingAPlanLeavesTheOneTimeItemsAsTheyAre() throws Exception {
        String plan = createPlan(2026);
        post("/api/one-time-items", BACK_PAYMENT);

        post("/api/plans/" + plan + "/copies", """
                {"targetYear":2027}""");
        mockMvc.perform(delete("/api/plans/{planId}", plan).cookie(alice)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/one-time-items").param("year", "2026").cookie(alice))
                .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/one-time-items").param("year", "2027").cookie(alice))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /** EP-7, EP-17 */
    @Test
    void anItemCanBeChangedIntoAnotherYearAndDeleted() throws Exception {
        String id = JsonPath.read(post("/api/one-time-items", TAX_REFUND), "$.id");

        mockMvc.perform(put("/api/one-time-items/{id}", id).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"INCOME","description":"Steuererstattung","amount":"800.00","date":"2027-01-05"}"""))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/one-time-items").param("year", "2026").cookie(alice))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get("/api/one-time-items").param("year", "2027").cookie(alice))
                .andExpect(jsonPath("$.items[0].id").value(id));

        mockMvc.perform(delete("/api/one-time-items/{id}", id).cookie(alice)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/one-time-items").param("year", "2027").cookie(alice))
                .andExpect(jsonPath("$.items.length()").value(0));
        assertThat(jdbcClient.sql("SELECT event_type FROM audit_log ORDER BY id").query(String.class).list())
                .containsExactly("OneTimeItemCreated", "OneTimeItemChanged", "OneTimeItemDeleted");
    }

    /** EP-4 */
    @Test
    void itemsOfSomeoneElseAreNeitherShownNorChangeable() throws Exception {
        String id = JsonPath.read(post("/api/one-time-items", BACK_PAYMENT), "$.id");

        mockMvc.perform(get("/api/one-time-items").param("year", "2026").cookie(bob))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(put("/api/one-time-items/{id}", id).cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON).content(BACK_PAYMENT))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/one-time-items/{id}", id).cookie(bob)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/one-time-items").param("year", "2026").cookie(alice))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    /** EP-1, EP-3 */
    @Test
    void invalidItemsAreRejected() throws Exception {
        for (String json : List.of(
                """
                        {"type":"INCOME","description":"Null","amount":"0.00","date":"2026-01-01"}""",
                """
                        {"type":"INCOME","description":"Zu früh","amount":"1.00","date":"1899-12-31"}""")) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/one-time-items")
                            .cookie(alice).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
    }
}
