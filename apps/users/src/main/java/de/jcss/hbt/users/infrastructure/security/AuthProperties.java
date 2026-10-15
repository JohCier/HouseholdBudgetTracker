package de.jcss.hbt.users.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * How sessions are issued. The backend checks the tokens with the same secret, cookie name
 * and issuer, so these must be configured identically in both apps.
 *
 * @param jwtSecret       secret the tokens are signed with (HMAC-SHA256), at least 32 bytes
 * @param cookieName      name of the cookie the token is set in
 * @param issuer          {@code iss} claim of the tokens
 * @param sessionDuration how long a session lasts before the user has to sign in again
 * @param secureCookie    whether the cookie is only sent over HTTPS; switch off only for plain HTTP
 */
@ConfigurationProperties("hbt.auth")
public record AuthProperties(String jwtSecret,
                             @DefaultValue("HBT_SESSION") String cookieName,
                             @DefaultValue("hbt-users") String issuer,
                             @DefaultValue("8h") Duration sessionDuration,
                             @DefaultValue("true") boolean secureCookie) {

    private static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        Objects.requireNonNull(jwtSecret, "hbt.auth.jwt-secret must be set");
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("hbt.auth.jwt-secret must be at least %d bytes".formatted(MIN_SECRET_BYTES));
        }
    }

    public SecretKey signingKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String toString() {
        return "AuthProperties{cookieName=%s, issuer=%s, sessionDuration=%s, secureCookie=%s}"
                .formatted(cookieName, issuer, sessionDuration, secureCookie);
    }
}
