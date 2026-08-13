CREATE TABLE assessment_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    answer_key VARCHAR(64) NOT NULL,
    assessment_id BIGINT NOT NULL,
    question_key VARCHAR(64) NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    answer_json JSON NOT NULL,
    audio_asset_id BIGINT NULL,
    correctness VARCHAR(32) NOT NULL,
    score DECIMAL(5,4) NOT NULL,
    evaluator_confidence DECIMAL(5,4) NOT NULL,
    hint_level TINYINT NOT NULL,
    duration_ms INT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_assessment_attempt_key UNIQUE (answer_key),
    CONSTRAINT uk_assessment_attempt_item UNIQUE (assessment_id, question_key),
    CONSTRAINT fk_assessment_attempt_session
        FOREIGN KEY (assessment_id) REFERENCES assessment_session (id)
);

CREATE INDEX idx_assessment_attempt_session_created
    ON assessment_attempt (assessment_id, created_at_utc);
