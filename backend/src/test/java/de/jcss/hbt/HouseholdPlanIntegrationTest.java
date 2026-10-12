package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full stack end to end: HTTP request in through the web layer, the write
 * side (application service, aggregate, repository) and the read side (query handler),
 * persisted in PostgreSQL, including the event listener running after the commit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HouseholdPlanIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcClient.sql("DELETE FROM household_plan").update();
        jdbcClient.sql("DELETE FROM audit_log").update();
    }

    @Test
    void tracksIncomeAndExpensesPerPlanAndComputesOverview() throws Exception {
        String plan2026 = createPlan(2026);
        String plan2027 = createPlan(2027);
        post("/api/plans/" + plan2026 + "/incomes", """
                {"description":"Gehalt","amount":"2000.00"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Miete","amount":"1000.00","category":"LEBENSUNTERHALT"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Konzert","amount":"300.00","category":"FREIZEIT_SPASS"}""");
        post("/api/plans/" + plan2027 + "/expenses", """
                {"description":"Urlaub","amount":"900.00","category":"FREIZEIT_SPASS"}""");

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan2026))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].description").value("Konzert"))
                .andExpect(jsonPath("$.totalIncome").value("2000.00"))
                .andExpect(jsonPath("$.totalExpense").value("1300.00"))
                .andExpect(jsonPath("$.balance").value("700.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false));

        mockMvc.perform(get("/api/plans").param("year", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan2027))
                .andExpect(jsonPath("$.totalExpense").value("900.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(true));
    }

    @Test
    void yearWithoutPlanIsNotFound() throws Exception {
        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    void secondPlanForTheSameYearIsAConflict() throws Exception {
        createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void removingAnItemUpdatesTheItemsAndIsAudited() throws Exception {
        String planId = createPlan(2026);
        String itemId = JsonPath.read(post("/api/plans/" + planId + "/incomes", """
                {"description":"Gehalt","amount":"1500.00"}"""), "$.id");

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, itemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/plans").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        assertThat(jdbcClient.sql("SELECT event_type, household_plan_id, budget_item_id FROM audit_log")
                .query().listOfRows())
                .containsExactly(Map.of(
                        "event_type", "BudgetItemRemoved",
                        "household_plan_id", UUID.fromString(planId),
                        "budget_item_id", UUID.fromString(itemId)));
        assertThat(jdbcClient.sql("SELECT occurred_at <= recorded_at FROM audit_log").query(Boolean.class).single())
                .as("removal happened before it was logged")
                .isTrue();
    }

    @Test
    void failedRemovalIsNotAudited() throws Exception {
        String planId = createPlan(2026);

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, UUID.randomUUID()))
                .andExpect(status().isNotFound());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM audit_log").query(Long.class).single()).isZero();
    }

    @Test
    void rejectsFractionsOfCents() throws Exception {
        String planId = createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/incomes", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"10.005"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    private String createPlan(int year) throws Exception {
        return JsonPath.read(post("/api/plans", """
                {"year":%d}""".formatted(year)), "$.id");
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
