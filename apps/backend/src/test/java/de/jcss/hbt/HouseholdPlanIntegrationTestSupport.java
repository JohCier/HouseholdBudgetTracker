package de.jcss.hbt;

import com.jayway.jsonpath.JsonPath;
import de.jcss.hbt.infrastructure.security.AuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full stack end to end: HTTP request in through the web layer, the write
 * side (application service, aggregate, repository) and the read side (query handler),
 * persisted in PostgreSQL, including the event listener running after the commit.
 * <p>
 * Requests carry a session cookie like the one the users app sets at login, signed here
 * with the same secret.
 */
abstract class HouseholdPlanIntegrationTestSupport {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected JdbcClient jdbcClient;

    @Autowired
    protected AuthProperties auth;

    protected MockMvc mockMvc;
    protected Cookie alice;
    protected Cookie bob;

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

    protected ResultActions changeItem(String planId, String itemId, String json)
            throws Exception {
        return mockMvc.perform(put("/api/plans/{planId}/items/{itemId}", planId, itemId)
                .cookie(alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected String overview(int year) throws Exception {
        return mockMvc.perform(get("/api/plans").param("year", String.valueOf(year)).cookie(alice))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected String createPlan(int year) throws Exception {
        return JsonPath.read(post("/api/plans", """
                {"year":%d}""".formatted(year)), "$.id");
    }

    protected String post(String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** A session cookie as set by the users app, signed with the shared secret. */
    protected Cookie session(UUID user, String issuer, Instant expiresAt) {
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
