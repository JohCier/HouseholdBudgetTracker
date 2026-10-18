package de.jcss.hbt.users.infrastructure.web;

import de.jcss.hbt.users.application.user.UserApplicationService;
import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.LoginLockedException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import de.jcss.hbt.users.infrastructure.security.SecurityConfig;
import de.jcss.hbt.users.infrastructure.security.SessionCookies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SessionCookies.class})
class AuthControllerTest {

    private static final UserId USER = UserId.of(UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserApplicationService users;

    @Test
    void registerReturns201WithTheUserAndStartsASession() throws Exception {
        when(users.register(new RegisterUserCommand(EmailAddress.of("max@example.org"), Password.of("geheim123"))))
                .thenReturn(USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Max@Example.org","password":"geheim123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER.value().toString()))
                .andExpect(jsonPath("$.email").value("max@example.org"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("HBT_SESSION=ey"), containsString("HttpOnly"),
                        containsString("SameSite=Strict"), containsString("Secure"), containsString("Path=/"))));
    }

    /** FM-2 */
    @Test
    void registeringATakenAddressReturns409() throws Exception {
        when(users.register(any()))
                .thenThrow(new EmailAddressAlreadyRegisteredException(EmailAddress.of("max@example.org")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"geheim123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.type").value("urn:hbt:email-already-registered"))
                .andExpect(jsonPath("$.detail").value("max@example.org is already registered"));
    }

    @Test
    void passwordTooShortOrMalformedAddressReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"kurz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                .andExpect(jsonPath("$.detail").value("password must have at least 8 characters"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max","password":"geheim123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(users);
    }

    @Test
    void missingFieldsAreListed() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(users);
    }

    @Test
    void loginReturnsTheUserAndStartsASession() throws Exception {
        when(users.authenticate(EmailAddress.of("max@example.org"), Password.of("geheim123"))).thenReturn(USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"geheim123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER.value().toString()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HBT_SESSION=ey")));
    }

    /** AN-10 */
    @Test
    void lockedAddressReturns429WithoutSession() throws Exception {
        when(users.authenticate(any(), any())).thenThrow(new LoginLockedException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"richtig-geheim"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.type").value("urn:hbt:too-many-login-attempts"));
    }

    /** FM-2 */
    @Test
    void wrongCredentialsReturn401WithoutSession() throws Exception {
        when(users.authenticate(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"max@example.org","password":"falsch-geheim"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-credentials"))
                .andExpect(jsonPath("$.detail").value("E-mail address or password is wrong"));
    }

    @Test
    void logoutRemovesTheSessionCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        allOf(containsString("HBT_SESSION=;"), containsString("Max-Age=0"))));
    }

    @Test
    void currentUserComesFromTheSession() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(token -> token.subject(USER.value().toString()).claim("email", "max@example.org"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER.value().toString()))
                .andExpect(jsonPath("$.email").value("max@example.org"));
    }

    @Test
    void currentUserWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Not signed in"));
    }
}
