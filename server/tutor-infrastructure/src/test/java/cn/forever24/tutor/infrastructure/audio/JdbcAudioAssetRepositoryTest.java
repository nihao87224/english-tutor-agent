package cn.forever24.tutor.infrastructure.audio;

import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAudioAssetRepositoryTest {
    @Test
    void persistsIdempotencyRetentionAndOwnerScope() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:audio_asset_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("lesson-session-schema-h2.sql")).execute(dataSource);
        var repository = new JdbcAudioAssetRepository(new JdbcTemplate(dataSource));
        var asset = new UserAudioAsset("usr_audio_1", "private/1.webm", "LESSON_ATTEMPT", "audio/webm",
                3, 1_000, "sha256:abc", AudioAssetStatus.READY, RawContentRetention.PROCESS_ONLY,
                Instant.parse("2026-08-22T01:00:00Z"), Instant.parse("2026-08-21T01:00:00Z"));

        repository.insert(new UserKey("usr-1"), asset, "idem", "request-hash");

        var restored = repository.findById(new UserKey("usr-1"), "usr_audio_1").orElseThrow();
        assertEquals(RawContentRetention.PROCESS_ONLY, restored.retention());
        assertEquals("request-hash", repository.findByIdempotencyKey(new UserKey("usr-1"), "idem")
                .orElseThrow().requestHash());
        assertEquals(1, repository.findExpired(Instant.parse("2026-08-22T01:00:01Z"), 100).size());
        assertTrue(repository.findById(new UserKey("usr-2"), "usr_audio_1").isEmpty());
    }
}
