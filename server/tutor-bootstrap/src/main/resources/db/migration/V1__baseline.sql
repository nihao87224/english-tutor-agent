CREATE TABLE app_schema_baseline (
    id BIGINT NOT NULL PRIMARY KEY,
    created_at_utc TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    note VARCHAR(255) NOT NULL
);

INSERT INTO app_schema_baseline (id, note)
VALUES (1, 'M0 technical baseline; no business tables yet');
