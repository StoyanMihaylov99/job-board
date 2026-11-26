CREATE TABLE app_user
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(254)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    display_name  VARCHAR(100)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT uq_app_user_email UNIQUE (email),
    -- emails are normalised to lower case by the application; enforce it so the unique constraint is case-insensitive
    CONSTRAINT chk_app_user_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE refresh_token
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- SHA-256 of the opaque token (hex); the raw token only ever lives in the user's HttpOnly cookie
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    -- ROTATED: replaced by a newer token; LOGOUT / REVOKED: deliberately ended (password change, theft detection)
    revoke_reason VARCHAR(10),

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_refresh_token_revocation CHECK (
        (revoked_at IS NULL AND revoke_reason IS NULL) OR
        (revoked_at IS NOT NULL AND revoke_reason IN ('ROTATED', 'LOGOUT', 'REVOKED')))
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);

-- Every application now belongs to a user. This intentionally fails if unowned rows exist,
-- rather than silently deleting or reassigning someone's data.
ALTER TABLE job_application
    ADD COLUMN owner_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE;

DROP INDEX idx_job_application_status;
CREATE INDEX idx_job_application_owner_status ON job_application (owner_id, status);
