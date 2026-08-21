package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioUploadApplicationServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final FakeStorage storage = new FakeStorage();
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private AudioUploadApplicationService service;

    @BeforeEach
    void setUp() {
        when(profiles.getPrivacySettings(any())).thenReturn(
                new PrivacySettings(RawContentRetention.PROCESS_ONLY, RawContentRetention.PROCESS_ONLY, 0));
        service = new AudioUploadApplicationService(repository, storage, profiles, () -> "1",
                Clock.fixed(Instant.parse("2026-08-21T01:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void validatesAndStoresProtectedAudioWithProcessOnlyDeadline() {
        var result = service.upload("usr-1", command("audio/webm", 1_000, new byte[]{1, 2, 3}), "idem");
        assertEquals("usr_audio_1", result.asset().audioAssetId());
        assertEquals("sha256:039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81",
                result.asset().contentHash());
        assertEquals(Instant.parse("2026-08-22T01:00:00Z"), result.asset().deleteAfter());
        assertTrue(result.asset().objectKey().startsWith("user-recordings/usr-1/2026/08/"));
        assertArrayEquals(new byte[]{1, 2, 3}, storage.content);
        assertTrue(service.upload("usr-1", command("audio/webm", 1_000, new byte[]{1, 2, 3}), "idem").replayed());
    }

    @Test
    void rejectsFormatDurationSizeHashAndChangedIdempotentPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upload("usr-1", command("audio/aac", 1_000, new byte[]{1}), "format"));
        assertThrows(IllegalArgumentException.class,
                () -> service.upload("usr-1", command("audio/webm", 99, new byte[]{1}), "duration"));
        assertThrows(IllegalArgumentException.class, () -> service.upload("usr-1",
                command("audio/webm", 1_000, new byte[(int) AudioUploadApplicationService.MAX_BYTES + 1]), "size"));
        assertThrows(IllegalArgumentException.class, () -> service.upload("usr-1",
                new UploadAudioCommand("LESSON_ATTEMPT", "audio/webm", 1_000, "0".repeat(64), new byte[]{1}), "hash"));
        service.upload("usr-1", command("audio/webm", 1_000, new byte[]{1}), "same");
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(AudioAssetApplicationException.class,
                () -> service.upload("usr-1", command("audio/webm", 1_000, new byte[]{2}), "same")).code());
    }

    @Test
    void mapsObjectFailureWithoutCreatingDatabaseAsset() {
        storage.fail = true;
        var error = assertThrows(AudioAssetApplicationException.class,
                () -> service.upload("usr-1", command("audio/ogg", 1_000, new byte[]{1}), "failed"));
        assertEquals("AUDIO_STORAGE_UNAVAILABLE", error.code());
        assertTrue(repository.assets.isEmpty());
    }

    @Test
    void retentionSweepDeletesExpiredObjectAndMarksMetadata() {
        var expired = new UserAudioAsset("usr_audio_old", "private/old.webm", "LESSON_ATTEMPT", "audio/webm",
                3, 1_000, "sha256:abc", cn.forever24.tutor.audio.AudioAssetStatus.READY,
                RawContentRetention.STORE, Instant.parse("2026-08-21T00:59:59Z"),
                Instant.parse("2026-08-20T01:00:00Z"));
        repository.assets.put(expired.audioAssetId(), expired);
        storage.content = new byte[]{1};

        int deleted = new AudioRetentionApplicationService(repository, storage,
                Clock.fixed(Instant.parse("2026-08-21T01:00:00Z"), ZoneOffset.UTC)).deleteExpired(100);

        assertEquals(1, deleted);
        assertNull(storage.content);
        assertFalse(repository.assets.containsKey("usr_audio_old"));
    }

    private static UploadAudioCommand command(String mime, long duration, byte[] bytes) {
        return new UploadAudioCommand("LESSON_ATTEMPT", mime, duration, null, bytes);
    }

    private static final class FakeStorage implements PrivateAudioObjectStorage {
        private byte[] content;
        private boolean fail;
        public void put(String key, byte[] value) { if (fail) throw new IllegalStateException("down"); content = value; }
        public byte[] read(String key) { return content; }
        public void delete(String key) { content = null; }
    }

    private static final class FakeRepository implements AudioAssetRepository {
        private final Map<String, AudioAssetStoreRecord> idempotency = new HashMap<>();
        private final Map<String, UserAudioAsset> assets = new HashMap<>();
        public Optional<AudioAssetStoreRecord> findByIdempotencyKey(UserKey userKey, String key) {
            return Optional.ofNullable(idempotency.get(userKey.value() + key));
        }
        public Optional<UserAudioAsset> findById(UserKey userKey, String id) { return Optional.ofNullable(assets.get(id)); }
        public void insert(UserKey userKey, UserAudioAsset asset, String key, String hash) {
            assets.put(asset.audioAssetId(), asset);
            idempotency.put(userKey.value() + key, new AudioAssetStoreRecord(hash, asset));
        }
        public void markDeleted(UserKey userKey, String id) { assets.remove(id); }
        public List<OwnedAudioAsset> findExpired(Instant now, int limit) {
            return assets.values().stream().filter(asset -> asset.deleteAfter() != null && !asset.deleteAfter().isAfter(now))
                    .limit(limit).map(asset -> new OwnedAudioAsset(new UserKey("usr-1"), asset)).toList();
        }
    }
}
