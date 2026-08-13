CREATE TABLE training_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_task_key VARCHAR(64) NOT NULL,
    started_at_utc DATETIME(3) NOT NULL,
    paused_at_utc DATETIME(3) NULL,
    completed_at_utc DATETIME(3) NULL,
    effective_seconds INT NOT NULL DEFAULT 0,
    summary_json JSON NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_training_session_key
        UNIQUE (session_key),
    CONSTRAINT uk_training_session_idempotency
        UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_training_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_training_session_plan
        FOREIGN KEY (plan_id) REFERENCES learning_plan (id)
);

CREATE INDEX idx_training_session_user_status
    ON training_session (user_id, status);

CREATE TABLE task_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_key VARCHAR(64) NOT NULL,
    session_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    input_type VARCHAR(16) NOT NULL,
    input_text TEXT NULL,
    audio_asset_id BIGINT NULL,
    answer_json JSON NOT NULL,
    hint_level TINYINT NOT NULL DEFAULT 0,
    result VARCHAR(32) NOT NULL,
    score DECIMAL(6,5) NULL,
    submitted_at_utc DATETIME(3) NOT NULL,
    evaluator_version VARCHAR(64) NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_task_attempt_key
        UNIQUE (attempt_key),
    CONSTRAINT uk_task_attempt_session_task_no
        UNIQUE (session_id, task_id, attempt_no),
    CONSTRAINT fk_task_attempt_session
        FOREIGN KEY (session_id) REFERENCES training_session (id),
    CONSTRAINT fk_task_attempt_task
        FOREIGN KEY (task_id) REFERENCES learning_task (id),
    CONSTRAINT fk_task_attempt_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_task_attempt_session_created
    ON task_attempt (session_id, submitted_at_utc);
