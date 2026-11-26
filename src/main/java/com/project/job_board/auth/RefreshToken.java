package com.project.job_board.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 10)
    private RevokeReason revokeReason;

    public enum RevokeReason {
        /** Replaced by a newer token during a normal refresh. */
        ROTATED,
        /** The user signed out with this token. */
        LOGOUT,
        /** All sessions ended: password change or suspected token theft. */
        REVOKED
    }

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() { return userId; }

    public Instant getExpiresAt() { return expiresAt; }

    public Instant getRevokedAt() { return revokedAt; }

    public RevokeReason getRevokeReason() { return revokeReason; }

    public boolean isRevoked() { return revokedAt != null; }

    public void revoke(Instant at, RevokeReason reason) {
        if (revokedAt == null) {
            revokedAt = at;
            revokeReason = reason;
        }
    }
}
