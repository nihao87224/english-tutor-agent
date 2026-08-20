ALTER TABLE learning_plan
    ADD COLUMN prescription_version BIGINT NULL AFTER adjustment_version,
    ADD COLUMN learner_timezone VARCHAR(64) NULL AFTER prescription_version,
    ADD COLUMN priority_goal VARCHAR(128) NULL AFTER learner_timezone,
    ADD COLUMN policy_version VARCHAR(32) NULL AFTER priority_goal,
    ADD COLUMN input_snapshot_json JSON NULL AFTER policy_version,
    ADD COLUMN reason_codes_json JSON NULL AFTER input_snapshot_json,
    ADD COLUMN expires_at_utc DATETIME(3) NULL AFTER reason_codes_json,
    ADD COLUMN supersedes_plan_id BIGINT NULL AFTER expires_at_utc,
    ADD CONSTRAINT fk_learning_plan_supersedes
        FOREIGN KEY (supersedes_plan_id) REFERENCES learning_plan (id);

CREATE UNIQUE INDEX uq_learning_plan_user_date_prescription_version
    ON learning_plan (user_id, plan_date, prescription_version);

CREATE INDEX idx_learning_plan_active_prescription
    ON learning_plan (user_id, plan_date, status, prescription_version);

ALTER TABLE learning_task
    ADD COLUMN block_type VARCHAR(32) NULL AFTER task_type,
    ADD COLUMN skill_unit_variant_id BIGINT NULL AFTER block_type,
    ADD COLUMN resource_version_id BIGINT NULL AFTER skill_unit_variant_id,
    ADD COLUMN episode_mapping_id BIGINT NULL AFTER resource_version_id,
    ADD COLUMN scaffolding_level VARCHAR(32) NULL AFTER episode_mapping_id,
    ADD COLUMN training_type VARCHAR(32) NULL AFTER scaffolding_level,
    ADD COLUMN expected_evidence_json JSON NULL AFTER training_type,
    ADD COLUMN fallback_resource_version_id BIGINT NULL AFTER expected_evidence_json,
    ADD COLUMN recommendation_factors_json JSON NULL AFTER fallback_resource_version_id,
    ADD CONSTRAINT fk_learning_task_skill_unit_variant
        FOREIGN KEY (skill_unit_variant_id) REFERENCES curriculum_skill_unit_variant (id),
    ADD CONSTRAINT fk_learning_task_resource_version
        FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    ADD CONSTRAINT fk_learning_task_episode_mapping
        FOREIGN KEY (episode_mapping_id) REFERENCES episode_mapping (id),
    ADD CONSTRAINT fk_learning_task_fallback_resource_version
        FOREIGN KEY (fallback_resource_version_id) REFERENCES learning_resource_version (id);

CREATE INDEX idx_learning_task_plan_status_sequence
    ON learning_task (plan_id, status, sequence_no);

CREATE INDEX idx_learning_task_resource_status
    ON learning_task (resource_version_id, status);

CREATE TABLE prescription_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    feedback_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    block_id VARCHAR(64) NULL,
    operation VARCHAR(32) NOT NULL,
    feedback_type VARCHAR(64) NOT NULL,
    available_minutes INT NULL,
    temporary_goal VARCHAR(500) NULL,
    note VARCHAR(1000) NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_plan_id BIGINT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    CONSTRAINT uk_prescription_feedback_key UNIQUE (feedback_key),
    CONSTRAINT uq_prescription_feedback_user_operation_idempotency
        UNIQUE (user_id, operation, idempotency_key),
    CONSTRAINT fk_prescription_feedback_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_prescription_feedback_plan
        FOREIGN KEY (plan_id) REFERENCES learning_plan (id),
    CONSTRAINT fk_prescription_feedback_response_plan
        FOREIGN KEY (response_plan_id) REFERENCES learning_plan (id),
    CONSTRAINT ck_prescription_feedback_operation
        CHECK (operation IN ('REGENERATE', 'SKIP'))
);

CREATE INDEX idx_prescription_feedback_plan_created
    ON prescription_feedback (plan_id, created_at_utc);
