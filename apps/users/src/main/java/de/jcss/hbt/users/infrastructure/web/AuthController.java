package de.jcss.hbt.users.infrastructure.web;

import de.jcss.hbt.users.application.user.UserApplicationService;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import de.jcss.hbt.users.infrastructure.security.SessionCookies;
import de.jcss.hbt.users.infrastructure.web.dto.LoginRequest;
import de.jcss.hbt.users.infrastructure.web.dto.RegisterUserRequest;
import de.jcss.hbt.users.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Registration and sessions as a REST API. Registering and signing in start a session by
 * setting the session cookie (see {@link SessionCookies}), which the browser then sends
 * along to this app and the backend.
 */
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "auth")
public class AuthController {

    private final UserApplicationService users;
    private final SessionCookies sessionCookies;

    public AuthController(UserApplicationService users, SessionCookies sessionCookies) {
        this.users = users;
        this.sessionCookies = sessionCookies;
    }

    /** Registers a new user and signs them in right away. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request, HttpServletResponse response) {
        EmailAddress email = EmailAddress.of(request.email());
        UserId id = users.register(new RegisterUserCommand(email, Password.of(request.password())));
        return startSession(id, email, response);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        EmailAddress email = EmailAddress.of(request.email());
        UserId id = users.authenticate(email, Password.of(request.password()));
        return startSession(id, email, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookies.end().toString());
    }

    /** The signed-in user; 401 without a valid session. */
    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE)
    public UserResponse currentUser(@AuthenticationPrincipal Jwt session) {
        return new UserResponse(UUID.fromString(session.getSubject()), session.getClaimAsString("email"));
    }

    private UserResponse startSession(UserId id, EmailAddress email, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookies.start(id, email).toString());
        return new UserResponse(id.value(), email.value());
    }
}
