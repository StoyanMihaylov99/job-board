package com.project.job_board.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/** Builds and reads the HttpOnly refresh-token cookie. JavaScript can never read it. */
@Component
public class RefreshCookies {

    private final SecurityProperties properties;

    public RefreshCookies(SecurityProperties properties) {
        this.properties = properties;
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.refreshCookie().name());
        return cookie != null ? cookie.getValue() : null;
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(properties.jwt().refreshTokenTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        SecurityProperties.RefreshCookie cookie = properties.refreshCookie();
        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite("Strict")
                .path(cookie.path());
    }
}
