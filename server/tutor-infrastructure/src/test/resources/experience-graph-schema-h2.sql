CREATE TABLE experience_season (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    season_key VARCHAR(8) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata_json VARCHAR(10000) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE experience_episode (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_key VARCHAR(8) NOT NULL UNIQUE,
    season_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    story_anchor VARCHAR(1000) NOT NULL,
    story_order_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    metadata_json VARCHAR(10000) NOT NULL,
    sequence_no INT NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (season_id) REFERENCES experience_season (id) ON DELETE CASCADE,
    CHECK (story_order_required = FALSE),
    CHECK (sequence_no >= 0)
);

CREATE TABLE experience_scene (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene_key VARCHAR(32) NOT NULL UNIQUE,
    episode_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    location VARCHAR(240) NOT NULL,
    story_context VARCHAR(1000) NOT NULL,
    character_state_json VARCHAR(10000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (episode_id) REFERENCES experience_episode (id) ON DELETE CASCADE
);

CREATE TABLE episode_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mapping_key VARCHAR(180) NOT NULL UNIQUE,
    variant_id BIGINT NOT NULL,
    episode_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    eligible_levels_json VARCHAR(10000) NOT NULL,
    learner_fit_json VARCHAR(10000) NOT NULL,
    story_transition_json VARCHAR(10000) NOT NULL,
    fit_inputs_json VARCHAR(10000) NOT NULL,
    fallback_mapping_id BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id),
    FOREIGN KEY (episode_id) REFERENCES experience_episode (id) ON DELETE CASCADE,
    FOREIGN KEY (scene_id) REFERENCES experience_scene (id) ON DELETE CASCADE,
    FOREIGN KEY (fallback_mapping_id) REFERENCES episode_mapping (id) ON DELETE SET NULL
);

CREATE TABLE episode_mapping_resource (
    mapping_id BIGINT NOT NULL,
    resource_version_id BIGINT NOT NULL,
    priority INT NOT NULL,
    PRIMARY KEY (mapping_id, resource_version_id),
    UNIQUE (mapping_id, priority),
    FOREIGN KEY (mapping_id) REFERENCES episode_mapping (id) ON DELETE CASCADE,
    FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    CHECK (priority >= 0)
);
