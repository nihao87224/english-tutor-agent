ALTER TABLE app_user
    ADD COLUMN email VARCHAR(320) NULL AFTER user_key,
    ADD COLUMN email_normalized VARCHAR(320) NULL AFTER email,
    ADD COLUMN password_hash VARCHAR(255) NULL AFTER email_normalized,
    ADD COLUMN email_verified_at_utc DATETIME(3) NULL AFTER password_hash,
    ADD COLUMN last_login_at_utc DATETIME(3) NULL AFTER email_verified_at_utc,
    ADD COLUMN disabled_at_utc DATETIME(3) NULL AFTER last_login_at_utc,
    ADD COLUMN deleted_at_utc DATETIME(3) NULL AFTER disabled_at_utc,
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 AFTER deleted_at_utc;

CREATE UNIQUE INDEX uk_app_user_email_normalized
    ON app_user (email_normalized);

CREATE INDEX idx_app_user_status
    ON app_user (status);
