ALTER TABLE training_session
    ADD COLUMN learning_task_id BIGINT NULL AFTER plan_id,
    ADD COLUMN resource_version_id BIGINT NULL AFTER learning_task_id,
    ADD COLUMN skill_unit_variant_id BIGINT NULL AFTER resource_version_id,
    ADD COLUMN episode_mapping_id BIGINT NULL AFTER skill_unit_variant_id,
    ADD COLUMN prescription_version BIGINT NULL AFTER episode_mapping_id,
    ADD COLUMN start_request_hash VARCHAR(64) NULL AFTER idempotency_key,
    ADD COLUMN current_step VARCHAR(64) NULL AFTER current_task_key,
    ADD COLUMN step_state_json JSON NULL AFTER current_step,
    ADD CONSTRAINT fk_training_session_learning_task
        FOREIGN KEY (learning_task_id) REFERENCES learning_task (id),
    ADD CONSTRAINT fk_training_session_resource_version
        FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    ADD CONSTRAINT fk_training_session_skill_unit_variant
        FOREIGN KEY (skill_unit_variant_id) REFERENCES curriculum_skill_unit_variant (id),
    ADD CONSTRAINT fk_training_session_episode_mapping
        FOREIGN KEY (episode_mapping_id) REFERENCES episode_mapping (id);

CREATE INDEX idx_training_session_lesson_resume
    ON training_session (user_id, type, status, updated_at_utc);

ALTER TABLE task_attempt
    ADD COLUMN idempotency_key VARCHAR(128) NULL AFTER attempt_key,
    ADD COLUMN attempt_status VARCHAR(48) NULL AFTER result,
    ADD COLUMN retry_of_attempt_id BIGINT NULL AFTER attempt_status,
    ADD COLUMN asr_transcript TEXT NULL AFTER retry_of_attempt_id,
    ADD COLUMN asr_confidence DECIMAL(6,5) NULL AFTER asr_transcript,
    ADD COLUMN transcript_confirmed BOOLEAN NOT NULL DEFAULT FALSE AFTER asr_confidence,
    ADD COLUMN evaluation_json JSON NULL AFTER transcript_confirmed,
    ADD COLUMN analysis_error_code VARCHAR(64) NULL AFTER evaluation_json,
    ADD COLUMN evaluator_prompt_version VARCHAR(64) NULL AFTER analysis_error_code,
    ADD COLUMN provider_trace_json JSON NULL AFTER evaluator_prompt_version,
    ADD CONSTRAINT fk_task_attempt_retry
        FOREIGN KEY (retry_of_attempt_id) REFERENCES task_attempt (id);

CREATE UNIQUE INDEX uq_task_attempt_session_idempotency
    ON task_attempt (session_id, idempotency_key);

CREATE INDEX idx_task_attempt_status_updated
    ON task_attempt (attempt_status, updated_at_utc);

CREATE INDEX idx_task_attempt_retry
    ON task_attempt (retry_of_attempt_id);
