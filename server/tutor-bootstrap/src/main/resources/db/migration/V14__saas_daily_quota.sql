CREATE TABLE quota_user_policy (
    user_id BIGINT NOT NULL PRIMARY KEY,
    daily_limit_override INT NULL,
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_quota_user_policy_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT chk_quota_user_policy_limit
        CHECK (daily_limit_override IS NULL OR daily_limit_override >= 0)
);

CREATE TABLE quota_daily_usage (
    user_id BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    daily_limit INT NOT NULL,
    bonus_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    reserved_count INT NOT NULL DEFAULT 0,
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id, quota_date),
    CONSTRAINT fk_quota_daily_usage_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT chk_quota_daily_usage_counts
        CHECK (daily_limit >= 0 AND bonus_count >= 0 AND used_count >= 0 AND reserved_count >= 0)
);

CREATE TABLE quota_reservation (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    request_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at_utc DATETIME(3) NOT NULL,
    committed_at_utc DATETIME(3) NULL,
    refunded_at_utc DATETIME(3) NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_quota_reservation_idempotency
        UNIQUE (user_id, request_type, idempotency_key),
    CONSTRAINT fk_quota_reservation_usage
        FOREIGN KEY (user_id, quota_date) REFERENCES quota_daily_usage (user_id, quota_date),
    CONSTRAINT chk_quota_reservation_status
        CHECK (status IN ('RESERVED', 'COMMITTED', 'REFUNDED'))
);

CREATE INDEX idx_quota_reservation_stale
    ON quota_reservation (status, expires_at_utc);

CREATE INDEX idx_quota_daily_usage_date
    ON quota_daily_usage (quota_date);
