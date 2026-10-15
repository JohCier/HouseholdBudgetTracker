package de.jcss.hbt.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * How the session tokens issued by the users app are checked. Both apps share the secret
 * the tokens are signed with (HMAC-SHA256), so it must be configured identically in both.
 *
 * @param jwtSecret  secret the tokens are signed with, at least 32 bytes
 * @param cookieName name of the cookie the browser sends the token in
 * @param issuer     expected {@code iss} claim of the tokens
 */
@ConfigurationProperties("hbt.auth")
public record AuthProperties(String jwtSecret,
                             @DefaultValue("HBT_SESSION") String cookieName,
                             @DefaultValue("hbt-users") String issuer) {

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
        return "AuthProperties{cookieName=%s, issuer=%s}".formatted(cookieName, issuer);
    }
}
