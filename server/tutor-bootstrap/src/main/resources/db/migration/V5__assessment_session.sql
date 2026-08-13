CREATE TABLE assessment_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    assessment_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_minutes INT NOT NULL,
    estimated_remaining_minutes INT NOT NULL,
    blueprint_version VARCHAR(32) NOT NULL,
    content_version VARCHAR(32) NOT NULL,
    started_at_utc DATETIME(3) NOT NULL,
    completed_at_utc DATETIME(3) NULL,
    elapsed_seconds INT NOT NULL,
    result_summary_json JSON NULL,
    confidence DECIMAL(5,4) NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_assessment_session_key UNIQUE (assessment_key),
    CONSTRAINT fk_assessment_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_assessment_session_user_active
    ON assessment_session (user_id, type, status);
