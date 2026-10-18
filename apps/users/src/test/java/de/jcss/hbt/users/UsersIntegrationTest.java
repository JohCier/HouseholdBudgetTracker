package de.jcss.hbt.users;

import de.jcss.hbt.users.application.user.LoginThrottle;
import de.jcss.hbt.users.domain.user.EmailAddress;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises registration and sessions end to end: HTTP in through the web layer, the
 * application service and the aggregate, stored in PostgreSQL with a real BCrypt hash,
 * and the session cookie checked by the security configuration.
 */
@IntegrationTest
class UsersIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private LoginThrottle loginThrottle;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        jdbcClient.sql("DELETE FROM users.app_user").update();
        // The failed sign-ins are kept in memory and outlive a single test.
        Stream.of("max@example.org", "moritz@example.org", "unknown@example.org")
                .map(EmailAddress::of)
                .forEach(loginThrottle::reset);
    }

    @Test
    void registeredUsersCanSignInAndAreKnownBySession() throws Exception {
        Cookie registration = sessionOf(credentials("register", "Max@Example.org", "richtig-geheim")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("max@example.org")));
        mockMvc.perform(get("/api/auth/me").cookie(registration))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("max@example.org"));

        Cookie login = sessionOf(credentials("login", "max@example.org", "richtig-geheim")
                .andExpect(status().isOk()));
        mockMvc.perform(get("/api/auth/me").cookie(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("max@example.org"));
    }

    @Test
    void passwordIsStoredAsBCryptHashOnly() throws Exception {
        credentials("register", "max@example.org", "richtig-geheim").andExpect(status().isCreated());

        String stored = jdbcClient.sql("SELECT password_hash FROM users.app_user").query(String.class).single();
        assertThat(stored).startsWith("$2").doesNotContain("richtig-geheim");
    }

    @Test
    void addressesAreUniqueRegardlessOfCase() throws Exception {
        credentials("register", "max@example.org", "richtig-geheim").andExpect(status().isCreated());

        credentials("register", "MAX@example.org", "anderes-geheim").andExpect(status().isConflict());
    }

    /** AN-10 */
    @Test
    void fiveFailedSignInsLockTheAddress() throws Exception {
        credentials("register", "max@example.org", "richtig-geheim").andExpect(status().isCreated());

        for (int i = 0; i < 5; i++) {
            credentials("login", "max@example.org", "falsch-geheim").andExpect(status().isUnauthorized());
        }

        credentials("login", "max@example.org", "richtig-geheim")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("urn:hbt:too-many-login-attempts"));
        credentials("login", "moritz@example.org", "falsch-geheim").andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        credentials("register", "max@example.org", "richtig-geheim").andExpect(status().isCreated());

        credentials("login", "max@example.org", "falsch-geheim").andExpect(status().isUnauthorized());
        credentials("login", "unknown@example.org", "richtig-geheim").andExpect(status().isUnauthorized());
    }

    @Test
    void staleSessionCookieDoesNotGetInTheWayOfSigningIn() throws Exception {
        credentials("register", "max@example.org", "richtig-geheim").andExpect(status().isCreated());
        Cookie stale = new Cookie("HBT_SESSION", "not-a-valid-token");

        mockMvc.perform(get("/api/auth/me").cookie(stale)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").cookie(stale)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"richtig-geheim"}"""))
                .andExpect(status().isOk());
    }

    private ResultActions credentials(String action, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/" + action)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private static Cookie sessionOf(ResultActions result) {
        Cookie cookie = result.andReturn().getResponse().getCookie("HBT_SESSION");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        return cookie;
    }
}
