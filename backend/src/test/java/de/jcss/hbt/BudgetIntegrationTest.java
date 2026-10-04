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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full hexagon end to end: HTTP request in through the web adapter,
 * through the application service and domain, persisted in PostgreSQL and read back.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BudgetIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcClient.sql("DELETE FROM budget_item").update();
    }

    @Test
    void tracksIncomeAndExpensesAndComputesMonthlyOverview() throws Exception {
        post("/api/budget/incomes", """
                {"description":"Gehalt","amount":"2000.00","bookingDate":"2026-10-01"}""");
        post("/api/budget/expenses", """
                {"description":"Miete","amount":"1000.00","category":"LEBENSUNTERHALT","bookingDate":"2026-10-03"}""");
        post("/api/budget/expenses", """
                {"description":"Konzert","amount":"300.00","category":"FREIZEIT_SPASS","bookingDate":"2026-10-20"}""");
        post("/api/budget/expenses", """
                {"description":"Urlaub","amount":"900.00","category":"FREIZEIT_SPASS","bookingDate":"2026-11-02"}""");

        mockMvc.perform(get("/api/budget/items").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].description").value("Konzert"));

        mockMvc.perform(get("/api/budget/overview").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value("2000.00"))
                .andExpect(jsonPath("$.totalExpense").value("1300.00"))
                .andExpect(jsonPath("$.balance").value("700.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false));

        mockMvc.perform(get("/api/budget/overview").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value("900.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(true));
    }

    @Test
    void removingAnItemUpdatesTheOverview() throws Exception {
        String response = post("/api/budget/incomes", """
                {"description":"Gehalt","amount":"1500.00","bookingDate":"2026-10-01"}""");
        String id = JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budget/items").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsFractionsOfCents() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Gehalt","amount":"10.005","bookingDate":"2026-10-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    private String post(String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
