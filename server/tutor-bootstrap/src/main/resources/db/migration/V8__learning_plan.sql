CREATE TABLE learning_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    plan_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    profile_version BIGINT NOT NULL,
    adjustment_version BIGINT NOT NULL,
    duration_minutes INT NOT NULL,
    focus_summary VARCHAR(500) NOT NULL,
    rationale VARCHAR(1000) NOT NULL,
    generation_source VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_learning_plan_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uk_learning_plan_key
        UNIQUE (plan_key),
    CONSTRAINT uq_learning_plan_user_date_profile_adjustment
        UNIQUE (user_id, plan_date, profile_version, adjustment_version)
);

CREATE TABLE learning_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_key VARCHAR(64) NOT NULL,
    plan_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    target_skills JSON NOT NULL,
    knowledge_targets JSON NULL,
    scenario VARCHAR(64) NULL,
    difficulty_band VARCHAR(32) NOT NULL,
    duration_minutes INT NOT NULL,
    content_ref VARCHAR(128) NOT NULL,
    task_payload_json JSON NOT NULL,
    evidence_policy_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_learning_task_plan
        FOREIGN KEY (plan_id) REFERENCES learning_plan (id),
    CONSTRAINT uk_learning_task_key
        UNIQUE (task_key),
    CONSTRAINT uq_learning_task_plan_sequence
        UNIQUE (plan_id, sequence_no)
);
