CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE curriculum_skill_unit_variant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_key VARCHAR(192) NOT NULL UNIQUE
);

CREATE TABLE learning_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_key VARCHAR(180) NOT NULL UNIQUE
);

CREATE TABLE learning_resource_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    semantic_version VARCHAR(32) NOT NULL,
    UNIQUE (resource_id, semantic_version),
    FOREIGN KEY (resource_id) REFERENCES learning_resource (id)
);

CREATE TABLE episode_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mapping_key VARCHAR(180) NOT NULL UNIQUE
);

CREATE TABLE learning_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_key VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    plan_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    profile_version BIGINT NOT NULL,
    adjustment_version BIGINT NOT NULL,
    prescription_version BIGINT,
    learner_timezone VARCHAR(64),
    priority_goal VARCHAR(128),
    policy_version VARCHAR(32),
    input_snapshot_json VARCHAR(10000),
    reason_codes_json VARCHAR(10000),
    expires_at_utc TIMESTAMP,
    supersedes_plan_id BIGINT,
    duration_minutes INT NOT NULL,
    focus_summary VARCHAR(500) NOT NULL,
    rationale VARCHAR(1000) NOT NULL,
    generation_source VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, plan_date, profile_version, adjustment_version),
    UNIQUE (user_id, plan_date, prescription_version),
    FOREIGN KEY (user_id) REFERENCES app_user (id),
    FOREIGN KEY (supersedes_plan_id) REFERENCES learning_plan (id)
);

CREATE TABLE learning_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_key VARCHAR(64) NOT NULL UNIQUE,
    plan_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    block_type VARCHAR(32),
    skill_unit_variant_id BIGINT,
    resource_version_id BIGINT,
    episode_mapping_id BIGINT,
    scaffolding_level VARCHAR(32),
    training_type VARCHAR(32),
    expected_evidence_json VARCHAR(10000),
    fallback_resource_version_id BIGINT,
    recommendation_factors_json VARCHAR(10000),
    target_skills VARCHAR(10000) NOT NULL,
    knowledge_targets VARCHAR(10000),
    scenario VARCHAR(64),
    difficulty_band VARCHAR(32) NOT NULL,
    duration_minutes INT NOT NULL,
    content_ref VARCHAR(256) NOT NULL,
    task_payload_json VARCHAR(10000) NOT NULL,
    evidence_policy_json VARCHAR(10000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (plan_id, sequence_no),
    FOREIGN KEY (plan_id) REFERENCES learning_plan (id),
    FOREIGN KEY (skill_unit_variant_id) REFERENCES curriculum_skill_unit_variant (id),
    FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    FOREIGN KEY (episode_mapping_id) REFERENCES episode_mapping (id),
    FOREIGN KEY (fallback_resource_version_id) REFERENCES learning_resource_version (id)
);

CREATE TABLE prescription_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_key VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    block_id VARCHAR(64),
    operation VARCHAR(32) NOT NULL,
    feedback_type VARCHAR(64) NOT NULL,
    available_minutes INT,
    temporary_goal VARCHAR(300),
    note VARCHAR(500),
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_plan_id BIGINT NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    UNIQUE (user_id, operation, idempotency_key),
    FOREIGN KEY (user_id) REFERENCES app_user (id),
    FOREIGN KEY (plan_id) REFERENCES learning_plan (id),
    FOREIGN KEY (response_plan_id) REFERENCES learning_plan (id)
);
