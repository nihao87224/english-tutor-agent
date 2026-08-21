ALTER TABLE learning_evidence
    ADD COLUMN skill_unit_variant_id BIGINT NULL AFTER attempt_id,
    ADD COLUMN resource_version_id BIGINT NULL AFTER skill_unit_variant_id,
    ADD COLUMN task_type VARCHAR(64) NULL AFTER resource_version_id,
    ADD COLUMN criteria_results_json JSON NULL AFTER task_type,
    ADD COLUMN retry_improves_evidence_id BIGINT NULL AFTER criteria_results_json,
    ADD COLUMN policy_version VARCHAR(64) NULL AFTER retry_improves_evidence_id,
    ADD CONSTRAINT fk_learning_evidence_variant FOREIGN KEY (skill_unit_variant_id) REFERENCES curriculum_skill_unit_variant (id),
    ADD CONSTRAINT fk_learning_evidence_resource_version FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    ADD CONSTRAINT fk_learning_evidence_retry FOREIGN KEY (retry_improves_evidence_id) REFERENCES learning_evidence (id);

CREATE UNIQUE INDEX uq_learning_evidence_attempt ON learning_evidence (attempt_id);

CREATE TABLE learning_evidence_skill (
    evidence_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    impact_score DECIMAL(6,5) NOT NULL,
    previous_estimate DECIMAL(6,5) NULL,
    next_estimate DECIMAL(6,5) NULL,
    PRIMARY KEY (evidence_id, skill_id),
    CONSTRAINT fk_learning_evidence_skill_evidence FOREIGN KEY (evidence_id) REFERENCES learning_evidence (id),
    CONSTRAINT fk_learning_evidence_skill_skill FOREIGN KEY (skill_id) REFERENCES curriculum_skill (id),
    CONSTRAINT ck_learning_evidence_skill_role CHECK (role IN ('TARGET', 'SUPPORTING')),
    CONSTRAINT ck_learning_evidence_skill_impact CHECK (impact_score >= 0 AND impact_score <= 1)
);

CREATE TABLE analysis_retry_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_key VARCHAR(128) NOT NULL,
    attempt_id BIGINT NOT NULL,
    job_type VARCHAR(48) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_run_at_utc DATETIME(6) NOT NULL,
    lease_owner VARCHAR(128) NULL,
    lease_until_utc DATETIME(6) NULL,
    last_error_code VARCHAR(64) NULL,
    created_at_utc DATETIME(6) NOT NULL,
    updated_at_utc DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uq_analysis_retry_job_key UNIQUE (job_key),
    CONSTRAINT uq_analysis_retry_job_attempt UNIQUE (attempt_id),
    CONSTRAINT fk_analysis_retry_job_attempt FOREIGN KEY (attempt_id) REFERENCES task_attempt (id),
    CONSTRAINT ck_analysis_retry_job_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED_FINAL'))
);

CREATE INDEX idx_analysis_retry_job_due ON analysis_retry_job (status, next_run_at_utc);
