package de.jcss.hbt;

import de.jcss.hbt.test.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Assets and liabilities end to end (VS-1 to VS-18): recording, ownership, the history with its
 * changes, copying into another year and the independence from household plans.
 */
@IntegrationTest
class NetWorthIntegrationTest extends HouseholdPlanIntegrationTestSupport {

    @BeforeEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM net_worth_statement").update();
    }

    /** VS-1, VS-6, VS-8, VS-12, VS-18: the example of the spec */
    @Test
    void recordsAssetsAndLiabilitiesAndComparesTheYears() throws Exception {
        entry(2025, "ASSET", "Depot", "20100.00");
        entry(2025, "LIABILITY", "Autokredit", "9600.00");
        entry(2026, "ASSET", "Girokonto", "3200.00");
        entry(2026, "LIABILITY", "Autokredit", "6300.00");
        entry(2026, "ASSET", "Tagesgeld", "6000.00");
        entry(2026, "ASSET", "ETF-Depot", "12500.00");
        String last = entry(2026, "LIABILITY", "Ratenkauf Küche", "1500.00");

        assertThat(JsonPath.<String>read(last, "$.netWorth")).isEqualTo("13900.00");
        mockMvc.perform(get("/api/net-worth/2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[*].name").value(contains("Girokonto", "Tagesgeld", "ETF-Depot")))
                .andExpect(jsonPath("$.liabilities[*].name").value(contains("Autokredit", "Ratenkauf Küche")))
                .andExpect(jsonPath("$.totalAssets").value("21700.00"))
                .andExpect(jsonPath("$.totalLiabilities").value("7800.00"))
                .andExpect(jsonPath("$.change.amount").value("3400.00"))
                .andExpect(jsonPath("$.change.percentage").value(32.38));
        mockMvc.perform(get("/api/net-worth/years").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[*].year").value(contains(2026, 2025)))
                .andExpect(jsonPath("$.years[1].netWorth").value("10500.00"))
                .andExpect(jsonPath("$.years[1].change").doesNotExist());
    }

    /** VS-5, VS-7 */
    @Test
    void changesAndRemovesEntriesAndTheYearVanishesWithItsLastEntry() throws Exception {
        String created = entry(2026, "ASSET", "Girokonto", "3200.00");
        String id = JsonPath.read(created, "$.assets[0].id");

        mockMvc.perform(put("/api/net-worth/2026/entries/{id}", id).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"LIABILITY","name":"Dispo","amount":"500.00"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(0))
                .andExpect(jsonPath("$.liabilities[0].id").value(id))
                .andExpect(jsonPath("$.netWorth").value("-500.00"))
                .andExpect(jsonPath("$.overindebted").value(true));

        mockMvc.perform(delete("/api/net-worth/2026/entries/{id}", id).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liabilities.length()").value(0))
                .andExpect(jsonPath("$.netWorth").value("0.00"));
        mockMvc.perform(get("/api/net-worth/years").cookie(alice))
                .andExpect(jsonPath("$.years.length()").value(0));
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM net_worth_statement").query(Long.class).single()).isZero();
    }

    /** VS-3 */
    @Test
    void othersNeitherSeeNorChangeTheEntries() throws Exception {
        String id = JsonPath.read(entry(2026, "ASSET", "Girokonto", "3200.00"), "$.assets[0].id");

        mockMvc.perform(get("/api/net-worth/2026").cookie(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.length()").value(0));
        mockMvc.perform(get("/api/net-worth/years").cookie(bob))
                .andExpect(jsonPath("$.years.length()").value(0));
        mockMvc.perform(delete("/api/net-worth/2026/entries/{id}", id).cookie(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/net-worth/2026/entries/{id}", id).cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSET","name":"Fremd","amount":"1.00"}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/net-worth/2026/copies").cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetYear":2027}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/net-worth/2026").cookie(alice))
                .andExpect(jsonPath("$.assets[0].name").value("Girokonto"));
    }

    /** VS-15, VS-16 */
    @Test
    void copiesIntoAnotherYearAndOverwritesOnlyWithPermission() throws Exception {
        entry(2026, "ASSET", "Girokonto", "3200.00");
        entry(2026, "LIABILITY", "Autokredit", "6300.00");

        copy(2026, """
                {"targetYear":2027}""").andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.assets[0].name").value("Girokonto"))
                .andExpect(jsonPath("$.liabilities[0].name").value("Autokredit"))
                .andExpect(jsonPath("$.change.amount").value("0.00"));

        String copied = JsonPath.read(mockMvc.perform(get("/api/net-worth/2027").cookie(alice))
                .andReturn().getResponse().getContentAsString(), "$.assets[0].id");
        mockMvc.perform(put("/api/net-worth/2027/entries/{id}", copied).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSET","name":"Girokonto","amount":"4000.00"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/net-worth/2026").cookie(alice))
                .andExpect(jsonPath("$.assets[0].amount").value("3200.00"));

        copy(2026, """
                {"targetYear":2027}""").andExpect(status().isConflict());
        mockMvc.perform(get("/api/net-worth/2027").cookie(alice))
                .andExpect(jsonPath("$.assets[0].amount").value("4000.00"));

        copy(2026, """
                {"targetYear":2027,"overwrite":true}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[0].amount").value("3200.00"));
        copy(2026, """
                {"targetYear":2026}""").andExpect(status().isBadRequest());
        copy(2025, """
                {"targetYear":2027}""").andExpect(status().isNotFound());
    }

    /** VS-4 */
    @Test
    void isIndependentOfTheHouseholdPlan() throws Exception {
        String plan = createPlan(2026);
        entry(2026, "ASSET", "Girokonto", "3200.00");

        mockMvc.perform(delete("/api/plans/{planId}", plan).cookie(alice)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/net-worth/2026").cookie(alice))
                .andExpect(jsonPath("$.assets[0].name").value("Girokonto"));
        mockMvc.perform(get("/api/plans/years").cookie(alice))
                .andExpect(jsonPath("$.years.length()").value(0));
    }

    @Test
    void logsTheEvents() throws Exception {
        entry(2026, "ASSET", "Girokonto", "3200.00");

        List<String> events = jdbcClient.sql("SELECT event_type FROM audit_log ORDER BY id").query(String.class).list();
        assertThat(events).containsExactly("NetWorthStatementCreated", "NetWorthEntryAdded");
    }

    private String entry(int year, String type, String name, String amount) throws Exception {
        return post("/api/net-worth/" + year + "/entries", """
                {"type":"%s","name":"%s","amount":"%s"}""".formatted(type, name, amount));
    }

    private ResultActions copy(int year, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/net-worth/{year}/copies", year).cookie(alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
