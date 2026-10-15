package de.jcss.hbt.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.web.util.WebUtils;

/**
 * Takes the session token from the HttpOnly cookie set by the users app at login, instead
 * of from an {@code Authorization} header, so that the token is never accessible to scripts.
 */
class CookieBearerTokenResolver implements BearerTokenResolver {

    private final String cookieName;

    CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public @Nullable String resolve(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
