package com.example.householdbudgettracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full hexagon end to end: HTTP request in through the web adapter,
 * through the application service and domain aggregate, persisted by the real
 * in-memory adapter and read back out again.
 */
@SpringBootTest
class BudgetIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void tracksIncomeAndExpensesAndComputesFiftyThirtyTwentyOverview() throws Exception {
        mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Gehalt\",\"amount\":2000.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budget/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Miete\",\"amount\":1000.00,\"category\":\"LEBENSUNTERHALT\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budget/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Konzert\",\"amount\":300.00,\"category\":\"FREIZEIT_SPASS\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/budget/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/budget/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(2000.00))
                .andExpect(jsonPath("$.totalExpense").value(1300.00))
                .andExpect(jsonPath("$.balance").value(700.00))
                .andExpect(jsonPath("$.expensesExceedIncome").value(false));
    }

    @Test
    void removingAnItemUpdatesTheOverview() throws Exception {
        String response = mockMvc.perform(post("/api/budget/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Gehalt\",\"amount\":1500.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/budget/items/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budget/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
