package de.jcss.hbt.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;

/**
 * Only signed-in users may use the API. Users sign in at the users app, which sets a
 * signed JWT (subject = user id) as an HttpOnly cookie; this app only verifies that token
 * and keeps no session of its own.
 * <p>
 * CSRF tokens are not needed: the cookie is {@code SameSite=Strict}, so browsers do not
 * send it along with requests triggered by other sites.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

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
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/api/docs/**", "/api/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(new CookieBearerTokenResolver(auth.cookieName()))
                        .authenticationEntryPoint(UNAUTHORIZED)
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(UNAUTHORIZED))
                .build();
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
