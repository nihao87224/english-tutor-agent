CREATE TABLE auth_refresh_session (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    client_type VARCHAR(32) NOT NULL,
    device_name VARCHAR(128) NULL,
    auth_version BIGINT NOT NULL,
    expires_at_utc DATETIME(3) NOT NULL,
    revoked_at_utc DATETIME(3) NULL,
    created_at_utc DATETIME(3) NOT NULL,
    last_used_at_utc DATETIME(3) NULL,
    replaced_by_id VARCHAR(64) NULL,
    CONSTRAINT uk_auth_refresh_session_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_refresh_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_auth_refresh_session_replacement
        FOREIGN KEY (replaced_by_id) REFERENCES auth_refresh_session (id)
);

CREATE INDEX idx_auth_refresh_session_user_revoked
    ON auth_refresh_session (user_id, revoked_at_utc);

CREATE INDEX idx_auth_refresh_session_expires
    ON auth_refresh_session (expires_at_utc);
