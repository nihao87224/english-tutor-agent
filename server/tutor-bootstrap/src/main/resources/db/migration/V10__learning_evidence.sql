CREATE TABLE learning_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    evidence_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    attempt_id BIGINT NULL,
    source VARCHAR(32) NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    skill_dimension VARCHAR(64) NOT NULL,
    knowledge_key VARCHAR(160) NULL,
    result VARCHAR(32) NOT NULL,
    raw_score DECIMAL(6,5) NOT NULL,
    weight DECIMAL(6,5) NOT NULL,
    independence DECIMAL(6,5) NOT NULL,
    transfer_level DECIMAL(6,5) NOT NULL,
    delay_days INT NOT NULL DEFAULT 0,
    evaluator_confidence DECIMAL(6,5) NOT NULL,
    metadata_json JSON NOT NULL,
    occurred_at_utc DATETIME(3) NOT NULL,
    consumed_at_utc DATETIME(3) NULL,
    CONSTRAINT uk_learning_evidence_key
        UNIQUE (evidence_key),
    CONSTRAINT fk_learning_evidence_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_learning_evidence_attempt
        FOREIGN KEY (attempt_id) REFERENCES task_attempt (id),
    CONSTRAINT ck_learning_evidence_raw_score
        CHECK (raw_score >= 0 AND raw_score <= 1),
    CONSTRAINT ck_learning_evidence_weight
        CHECK (weight >= 0 AND weight <= 1),
    CONSTRAINT ck_learning_evidence_independence
        CHECK (independence >= 0 AND independence <= 1),
    CONSTRAINT ck_learning_evidence_transfer_level
        CHECK (transfer_level >= 0 AND transfer_level <= 1),
    CONSTRAINT ck_learning_evidence_confidence
        CHECK (evaluator_confidence >= 0 AND evaluator_confidence <= 1)
);

CREATE INDEX idx_learning_evidence_user_occurred
    ON learning_evidence (user_id, occurred_at_utc);

CREATE INDEX idx_learning_evidence_user_skill
    ON learning_evidence (user_id, skill_dimension);

CREATE INDEX idx_learning_evidence_user_knowledge
    ON learning_evidence (user_id, knowledge_key);
