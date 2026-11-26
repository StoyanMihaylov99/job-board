package com.project.job_board.auth.dto;

import com.project.job_board.user.dto.UserResponse;

/** Access token for the Authorization header. The refresh token is only ever sent as an HttpOnly cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}
