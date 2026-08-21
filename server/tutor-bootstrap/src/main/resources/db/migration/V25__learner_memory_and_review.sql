CREATE TABLE learner_error_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    error_key CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    error_tag VARCHAR(64) NOT NULL,
    related_skill_id BIGINT NOT NULL,
    frequency INT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    last_attempt_id BIGINT NOT NULL,
    last_occurred_at_utc DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    metadata_json JSON NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_learner_error_memory_key UNIQUE (error_key),
    CONSTRAINT uq_learner_error_memory_user_tag_skill UNIQUE (user_id, error_tag, related_skill_id),
    CONSTRAINT fk_learner_error_memory_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_learner_error_memory_skill FOREIGN KEY (related_skill_id) REFERENCES curriculum_skill (id),
    CONSTRAINT fk_learner_error_memory_attempt FOREIGN KEY (last_attempt_id) REFERENCES task_attempt (id),
    CONSTRAINT ck_learner_error_memory_frequency CHECK (frequency > 0),
    CONSTRAINT ck_learner_error_memory_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_learner_error_memory_status CHECK (status IN ('ACTIVE', 'RESOLVED'))
);

CREATE INDEX idx_learner_error_memory_user_status ON learner_error_memory (user_id, status, frequency DESC);

CREATE TABLE learner_expression_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expression_key CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    normalized_expression VARCHAR(300) NOT NULL,
    state VARCHAR(16) NOT NULL,
    confidence DECIMAL(6,5) NOT NULL,
    last_attempt_id BIGINT NOT NULL,
    last_used_at_utc DATETIME(3) NOT NULL,
    metadata_json JSON NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_learner_expression_memory_key UNIQUE (expression_key),
    CONSTRAINT uq_learner_expression_memory_user_expression UNIQUE (user_id, normalized_expression),
    CONSTRAINT fk_learner_expression_memory_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_learner_expression_memory_attempt FOREIGN KEY (last_attempt_id) REFERENCES task_attempt (id),
    CONSTRAINT ck_learner_expression_memory_state CHECK (state IN ('UNDERSTOOD', 'PROMPTED', 'INDEPENDENT', 'TRANSFERRED')),
    CONSTRAINT ck_learner_expression_memory_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX idx_learner_expression_memory_user_state ON learner_expression_memory (user_id, state, last_used_at_utc DESC);

CREATE TABLE learner_review_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_key CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    skill_id BIGINT NULL,
    expression_memory_id BIGINT NULL,
    due_at_utc DATETIME(3) NOT NULL,
    forgetting_risk DECIMAL(6,5) NOT NULL,
    last_recall_quality VARCHAR(16) NOT NULL,
    review_count INT NOT NULL DEFAULT 0,
    policy_version VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_learner_review_state_key UNIQUE (review_key),
    CONSTRAINT fk_learner_review_state_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_learner_review_state_skill FOREIGN KEY (skill_id) REFERENCES curriculum_skill (id),
    CONSTRAINT fk_learner_review_state_expression FOREIGN KEY (expression_memory_id) REFERENCES learner_expression_memory (id),
    CONSTRAINT ck_learner_review_state_target CHECK ((skill_id IS NOT NULL AND expression_memory_id IS NULL) OR (skill_id IS NULL AND expression_memory_id IS NOT NULL)),
    CONSTRAINT ck_learner_review_state_type CHECK (target_type IN ('SKILL', 'EXPRESSION')),
    CONSTRAINT ck_learner_review_state_risk CHECK (forgetting_risk >= 0 AND forgetting_risk <= 1),
    CONSTRAINT ck_learner_review_state_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_learner_review_state_due ON learner_review_state (user_id, status, due_at_utc);
