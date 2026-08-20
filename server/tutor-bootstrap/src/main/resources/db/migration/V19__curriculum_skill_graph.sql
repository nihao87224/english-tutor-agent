CREATE TABLE curriculum_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_key VARCHAR(128) NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(64) NOT NULL,
    cefr_min VARCHAR(8) NOT NULL,
    cefr_max VARCHAR(8) NOT NULL,
    importance INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_curriculum_skill_key UNIQUE (skill_key),
    CONSTRAINT ck_curriculum_skill_importance CHECK (importance >= 0 AND importance <= 100),
    CONSTRAINT ck_curriculum_skill_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_curriculum_skill_category_status
    ON curriculum_skill (category, status);

CREATE TABLE curriculum_skill_edge (
    parent_skill_id BIGINT NOT NULL,
    child_skill_id BIGINT NOT NULL,
    edge_type VARCHAR(32) NOT NULL,
    PRIMARY KEY (parent_skill_id, child_skill_id, edge_type),
    CONSTRAINT fk_curriculum_skill_edge_parent
        FOREIGN KEY (parent_skill_id) REFERENCES curriculum_skill (id) ON DELETE CASCADE,
    CONSTRAINT fk_curriculum_skill_edge_child
        FOREIGN KEY (child_skill_id) REFERENCES curriculum_skill (id) ON DELETE CASCADE,
    CONSTRAINT ck_curriculum_skill_edge_not_self CHECK (parent_skill_id <> child_skill_id),
    CONSTRAINT ck_curriculum_skill_edge_type CHECK (edge_type IN ('PARENT'))
);

CREATE TABLE curriculum_skill_unit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_unit_key VARCHAR(160) NOT NULL,
    communication_goal VARCHAR(500) NOT NULL,
    review_template_json JSON NOT NULL,
    semantic_version VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_curriculum_skill_unit_key UNIQUE (skill_unit_key),
    CONSTRAINT ck_curriculum_skill_unit_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE curriculum_skill_unit_variant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    variant_key VARCHAR(192) NOT NULL,
    skill_unit_id BIGINT NOT NULL,
    cefr_level VARCHAR(8) NOT NULL,
    communication_complexity INT NOT NULL,
    estimated_min_minutes INT NOT NULL,
    estimated_max_minutes INT NOT NULL,
    training_types_json JSON NOT NULL,
    scaffolding_levels_json JSON NOT NULL,
    common_error_tags_json JSON NOT NULL,
    completion_policy_json JSON NOT NULL,
    retry_policy_json JSON NOT NULL,
    mastery_policy_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_curriculum_skill_unit_variant_key UNIQUE (variant_key),
    CONSTRAINT fk_curriculum_variant_unit
        FOREIGN KEY (skill_unit_id) REFERENCES curriculum_skill_unit (id) ON DELETE CASCADE,
    CONSTRAINT ck_curriculum_variant_complexity
        CHECK (communication_complexity >= 1 AND communication_complexity <= 5),
    CONSTRAINT ck_curriculum_variant_duration
        CHECK (estimated_min_minutes >= 1 AND estimated_max_minutes >= estimated_min_minutes),
    CONSTRAINT ck_curriculum_variant_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_curriculum_variant_level_status
    ON curriculum_skill_unit_variant (cefr_level, status);

CREATE INDEX idx_curriculum_variant_unit_status
    ON curriculum_skill_unit_variant (skill_unit_id, status);

CREATE TABLE curriculum_variant_target_skill (
    variant_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (variant_id, skill_id, role),
    CONSTRAINT fk_curriculum_variant_skill_variant
        FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id) ON DELETE CASCADE,
    CONSTRAINT fk_curriculum_variant_skill_skill
        FOREIGN KEY (skill_id) REFERENCES curriculum_skill (id),
    CONSTRAINT ck_curriculum_variant_skill_role CHECK (role IN ('TARGET', 'SUPPORTING'))
);

CREATE INDEX idx_curriculum_variant_target_skill_lookup
    ON curriculum_variant_target_skill (skill_id, role, variant_id);

CREATE TABLE curriculum_variant_prerequisite (
    variant_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    minimum_mastery DECIMAL(6,5) NOT NULL,
    minimum_confidence DECIMAL(6,5) NOT NULL,
    PRIMARY KEY (variant_id, skill_id),
    CONSTRAINT fk_curriculum_prerequisite_variant
        FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id) ON DELETE CASCADE,
    CONSTRAINT fk_curriculum_prerequisite_skill
        FOREIGN KEY (skill_id) REFERENCES curriculum_skill (id),
    CONSTRAINT ck_curriculum_prerequisite_mastery
        CHECK (minimum_mastery >= 0 AND minimum_mastery <= 1),
    CONSTRAINT ck_curriculum_prerequisite_confidence
        CHECK (minimum_confidence >= 0 AND minimum_confidence <= 1)
);

CREATE TABLE curriculum_evidence_criterion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    criterion_key VARCHAR(128) NOT NULL,
    variant_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    weight DECIMAL(6,5) NOT NULL,
    required BOOLEAN NOT NULL,
    sequence_no INT NOT NULL,
    CONSTRAINT uk_curriculum_evidence_criterion UNIQUE (variant_id, criterion_key),
    CONSTRAINT fk_curriculum_evidence_variant
        FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id) ON DELETE CASCADE,
    CONSTRAINT ck_curriculum_evidence_weight CHECK (weight > 0 AND weight <= 1),
    CONSTRAINT ck_curriculum_evidence_sequence CHECK (sequence_no >= 0)
);

CREATE INDEX idx_curriculum_evidence_variant_sequence
    ON curriculum_evidence_criterion (variant_id, sequence_no);
