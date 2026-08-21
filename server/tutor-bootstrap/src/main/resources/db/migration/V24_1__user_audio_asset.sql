CREATE TABLE user_audio_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_key VARCHAR(96) NOT NULL,
    user_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    mime_type VARCHAR(96) NOT NULL,
    byte_length BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    content_hash VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL,
    retention_mode VARCHAR(24) NOT NULL,
    delete_after_utc DATETIME(6) NULL,
    created_at_utc DATETIME(6) NOT NULL,
    updated_at_utc DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_audio_asset_key UNIQUE (asset_key),
    CONSTRAINT uq_user_audio_object_key UNIQUE (object_key),
    CONSTRAINT uq_user_audio_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_user_audio_asset_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ck_user_audio_byte_length CHECK (byte_length > 0 AND byte_length <= 52428800),
    CONSTRAINT ck_user_audio_duration CHECK (duration_ms >= 100 AND duration_ms <= 600000)
);

CREATE INDEX idx_user_audio_retention
    ON user_audio_asset (status, delete_after_utc);
