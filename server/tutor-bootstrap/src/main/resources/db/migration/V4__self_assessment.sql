CREATE TABLE self_assessment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    self_assessment_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    listening_level TINYINT NOT NULL,
    speaking_level TINYINT NOT NULL,
    reading_level TINYINT NOT NULL,
    writing_level TINYINT NOT NULL,
    answers_json JSON NOT NULL,
    estimated_band VARCHAR(32) NOT NULL,
    completed_at_utc DATETIME(3) NOT NULL,
    CONSTRAINT uk_self_assessment_key UNIQUE (self_assessment_key),
    CONSTRAINT fk_self_assessment_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);
