CREATE TABLE learner_skill_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dimension VARCHAR(64) NOT NULL,
    estimate DECIMAL(6,5) NOT NULL,
    confidence DECIMAL(6,5) NOT NULL,
    level VARCHAR(32) NOT NULL,
    trend VARCHAR(32) NOT NULL,
    evidence_count INT NOT NULL,
    last_evidence_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_learner_skill_state_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_learner_skill_state_user_dimension
        UNIQUE (user_id, dimension),
    CONSTRAINT ck_learner_skill_state_estimate
        CHECK (estimate >= 0 AND estimate <= 1),
    CONSTRAINT ck_learner_skill_state_confidence
        CHECK (confidence >= 0 AND confidence <= 1)
);
