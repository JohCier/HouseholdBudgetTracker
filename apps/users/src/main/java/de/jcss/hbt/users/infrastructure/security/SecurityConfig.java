package de.jcss.hbt.users.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;

/**
 * Registering, signing in and out are open to everyone; only {@code GET /api/auth/me}
 * requires a session, i.e. a valid token in the session cookie (see {@link SessionCookies}).
 * <p>
 * The cookie is only looked at for that endpoint: an expired cookie still sent by the
 * browser must not get in the way of signing in again.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private static final RequestMatcher CURRENT_USER =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/auth/me");

    /** Answers unauthenticated requests with a problem detail, like all other errors. */
    private static final AuthenticationEntryPoint UNAUTHORIZED = (request, response, exception) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Not signed in"}""");
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties auth) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(CURRENT_USER).authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(sessionCookieOfCurrentUserRequests(auth.cookieName()))
                        .authenticationEntryPoint(UNAUTHORIZED)
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(UNAUTHORIZED))
                .build();
    }

    private static BearerTokenResolver sessionCookieOfCurrentUserRequests(String cookieName) {
        return request -> {
            if (!CURRENT_USER.matches(request)) {
                return null;
            }
            Cookie cookie = WebUtils.getCookie(request, cookieName);
            return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
        };
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties auth) {
        return NimbusJwtEncoder.withSecretKey(auth.signingKey()).build();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties auth) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(auth.signingKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(auth.issuer()));
        return decoder;
    }
}
