package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import de.jcss.hbt.infrastructure.security.AuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
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
 * <p>
 * Requests carry a session cookie like the one the users app sets at login, signed here
 * with the same secret.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
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
                {"description":"Gehalt","amount":"2000.00"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Miete","amount":"1000.00","category":"LEBENSUNTERHALT"}""");
        post("/api/plans/" + plan2026 + "/expenses", """
                {"description":"Konzert","amount":"300.00","category":"FREIZEIT_SPASS"}""");
        post("/api/plans/" + plan2027 + "/expenses", """
                {"description":"Urlaub","amount":"900.00","category":"FREIZEIT_SPASS"}""");

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan2026))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].description").value("Konzert"))
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
                {"description":"Gehalt","amount":"1500.00"}"""), "$.id");

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/incomes", alicesPlan)
                        .cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Fremd","amount":"1.00"}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", alicesPlan, alicesItem).cookie(bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(alicesItem));
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
                {"description":"Gehalt","amount":"1500.00"}"""), "$.id");

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, itemId).cookie(alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/plans").param("year", "2026").cookie(alice))
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

        mockMvc.perform(delete("/api/plans/{planId}/items/{itemId}", planId, UUID.randomUUID()).cookie(alice))
                .andExpect(status().isNotFound());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM audit_log").query(Long.class).single()).isZero();
    }

    @Test
    void rejectsFractionsOfCents() throws Exception {
        String planId = createPlan(2026);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/plans/{planId}/incomes", planId)
                        .cookie(alice)
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
