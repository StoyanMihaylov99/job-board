package com.project.job_board.auth;

import com.project.job_board.auth.dto.LoginRequest;
import com.project.job_board.auth.dto.RegisterRequest;
import com.project.job_board.user.AppUser;
import com.project.job_board.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * A refresh token that was rotated this recently may be presented again (e.g. two tabs refreshing at
     * the same moment) without being treated as theft.
     */
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private static final String INVALID_CREDENTIALS = "Email or password is incorrect.";
    private static final String SESSION_EXPIRED = "Your session has ended. Sign in again.";

    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;
    private final Clock clock;
    /** Compared against when the email is unknown, so response time doesn't reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthService(AppUserRepository users, RefreshTokenRepository refreshTokens, TokenService tokens,
                       PasswordEncoder passwordEncoder, SecurityProperties properties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** A signed-in session: the user, a short-lived access token and the raw refresh token for the cookie. */
    public record Session(AppUser user, String accessToken, String refreshToken) {
    }

    @Transactional
    public Session register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        AppUser user = users.saveAndFlush(new AppUser(
                email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return startSession(user);
    }

    @Transactional
    public Session login(LoginRequest request) {
        AppUser user = users.findByEmail(normalizeEmail(request.email())).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return startSession(user);
    }

    /**
     * Rotates the refresh token: the presented one is revoked and a new pair is issued. Presenting an
     * already-revoked token (outside the grace window) means it was copied, so every session of that user is ended.
     */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public Session refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationFailedException(SESSION_EXPIRED);
        }
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHash(tokens.hash(rawRefreshToken))
                .orElseThrow(() -> new AuthenticationFailedException(SESSION_EXPIRED));

        if (current.isRevoked()) {
            boolean concurrentRefresh = current.getRevokeReason() == RefreshToken.RevokeReason.ROTATED
                    && !current.getRevokedAt().isBefore(now.minus(REUSE_GRACE));
            if (!concurrentRefresh) {
                if (current.getRevokeReason() == RefreshToken.RevokeReason.ROTATED) {
                    log.warn("Rotated refresh token reused for user {}; revoking all sessions", current.getUserId());
                    refreshTokens.revokeAllForUser(current.getUserId(), now);
                }
                throw new AuthenticationFailedException(SESSION_EXPIRED);
            }
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new AuthenticationFailedException(SESSION_EXPIRED);
        }

        AppUser user = users.findById(current.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException(SESSION_EXPIRED));
        current.revoke(now, RefreshToken.RevokeReason.ROTATED);
        return startSession(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(tokens.hash(rawRefreshToken))
                .ifPresent(token -> token.revoke(clock.instant(), RefreshToken.RevokeReason.LOGOUT));
    }

    /** Ends every session of the user (all devices) and starts a fresh one for the caller. */
    @Transactional
    public Session restartSessions(AppUser user) {
        refreshTokens.revokeAllForUser(user.getId(), clock.instant());
        return startSession(user);
    }

    private Session startSession(AppUser user) {
        String refreshToken = tokens.newRefreshTokenValue();
        Instant expiresAt = clock.instant().plus(properties.jwt().refreshTokenTtl());
        refreshTokens.save(new RefreshToken(user.getId(), tokens.hash(refreshToken), expiresAt));
        return new Session(user, tokens.createAccessToken(user), refreshToken);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
