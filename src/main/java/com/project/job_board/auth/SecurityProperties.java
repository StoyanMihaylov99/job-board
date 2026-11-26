package com.project.job_board.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, RefreshCookie refreshCookie) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record RefreshCookie(String name, String path, boolean secure) {
    }
}
