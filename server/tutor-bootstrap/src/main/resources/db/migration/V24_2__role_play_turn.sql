CREATE TABLE role_play_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    turn_key VARCHAR(128) NOT NULL,
    session_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    task_key VARCHAR(128) NOT NULL,
    learner_text TEXT NULL,
    reply_text TEXT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    prompt_version VARCHAR(64) NULL,
    provider_id VARCHAR(128) NULL,
    model_id VARCHAR(128) NULL,
    trace_id VARCHAR(128) NULL,
    error_code VARCHAR(64) NULL,
    accepted_at_utc DATETIME(6) NOT NULL,
    completed_at_utc DATETIME(6) NULL,
    created_at_utc DATETIME(6) NOT NULL,
    updated_at_utc DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_role_play_turn_session FOREIGN KEY (session_id) REFERENCES training_session (id),
    CONSTRAINT fk_role_play_turn_attempt FOREIGN KEY (attempt_id) REFERENCES task_attempt (id),
    CONSTRAINT fk_role_play_turn_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_role_play_turn_session_turn UNIQUE (session_id, turn_key),
    CONSTRAINT uq_role_play_turn_session_idempotency UNIQUE (session_id, idempotency_key),
    CONSTRAINT uq_role_play_turn_attempt UNIQUE (attempt_id),
    CONSTRAINT chk_role_play_turn_status CHECK (
        status IN ('ACCEPTED', 'AWAITING_TRANSCRIPT', 'COMPLETED', 'FAILED_RETRYABLE', 'FAILED_FINAL'))
);

CREATE INDEX idx_role_play_turn_reconcile
    ON role_play_turn (user_id, session_id, accepted_at_utc);

CREATE INDEX idx_role_play_turn_retry
    ON role_play_turn (status, updated_at_utc);
