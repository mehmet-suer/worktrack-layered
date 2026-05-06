CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    CONSTRAINT fk_refresh_token_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_token_user_revoked_at ON refresh_token(user_id, revoked_at);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
