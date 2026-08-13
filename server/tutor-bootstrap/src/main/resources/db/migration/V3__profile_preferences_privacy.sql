ALTER TABLE user_learning_profile
    ADD COLUMN reminder_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER correction_preference,
    ADD COLUMN raw_text_retention VARCHAR(32) NOT NULL DEFAULT 'STORE' AFTER reminder_enabled,
    ADD COLUMN raw_audio_retention VARCHAR(32) NOT NULL DEFAULT 'STORE' AFTER raw_text_retention,
    ADD COLUMN raw_audio_retention_days INT NOT NULL DEFAULT 0 AFTER raw_audio_retention;
