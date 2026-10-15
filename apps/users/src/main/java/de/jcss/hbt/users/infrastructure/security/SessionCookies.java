package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.UserId;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Creates the session of a signed-in user: a signed JWT (subject = user id, plus the
 * e-mail address) in an HttpOnly cookie, so scripts cannot read it. {@code SameSite=Strict}
 * keeps browsers from sending it along with requests started by other sites (CSRF).
 * Sessions are not stored; they end when the token expires or the cookie is removed.
 */
@Component
public class SessionCookies {

    static final String EMAIL_CLAIM = "email";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties auth;

    SessionCookies(JwtEncoder jwtEncoder, AuthProperties auth) {
        this.jwtEncoder = jwtEncoder;
        this.auth = auth;
    }

    public ResponseCookie start(UserId user, EmailAddress email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(auth.issuer())
                .subject(user.value().toString())
                .claim(EMAIL_CLAIM, email.value())
                .issuedAt(now)
                .expiresAt(now.plus(auth.sessionDuration()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return cookie(token, auth.sessionDuration());
    }

    /** Tells the browser to drop the session cookie. */
    public ResponseCookie end() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(auth.cookieName(), value)
                .httpOnly(true)
                .secure(auth.secureCookie())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
