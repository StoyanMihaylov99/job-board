package com.project.job_board.user;

import com.project.job_board.auth.AuthService;
import com.project.job_board.auth.CurrentUserId;
import com.project.job_board.auth.RefreshCookies;
import com.project.job_board.auth.TokenService;
import com.project.job_board.auth.dto.AuthResponse;
import com.project.job_board.user.dto.ChangePasswordRequest;
import com.project.job_board.user.dto.UpdateProfileRequest;
import com.project.job_board.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The signed-in user's own profile. */
@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshCookies refreshCookies;

    public ProfileController(UserService userService, TokenService tokenService, RefreshCookies refreshCookies) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshCookies = refreshCookies;
    }

    @GetMapping
    public UserResponse me(@CurrentUserId UUID userId) {
        return userService.get(userId);
    }

    @PatchMapping
    public UserResponse updateProfile(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PutMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@CurrentUserId UUID userId,
                                                       @Valid @RequestBody ChangePasswordRequest request) {
        AuthService.Session session = userService.changePassword(userId, request);
        AuthResponse body = AuthResponse.bearer(
                session.accessToken(), tokenService.accessTokenTtlSeconds(), UserResponse.from(session.user()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
