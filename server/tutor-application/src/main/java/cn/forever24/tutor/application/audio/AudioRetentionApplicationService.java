package cn.forever24.tutor.application.audio;

import java.time.Clock;
import java.util.Objects;

public final class AudioRetentionApplicationService {
    private static final System.Logger LOGGER = System.getLogger(AudioRetentionApplicationService.class.getName());
    private final AudioAssetRepository repository;
    private final PrivateAudioObjectStorage storage;
    private final Clock clock;

    public AudioRetentionApplicationService(
            AudioAssetRepository repository, PrivateAudioObjectStorage storage, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.storage = Objects.requireNonNull(storage);
        this.clock = Objects.requireNonNull(clock);
    }

    public int deleteExpired(int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("limit must be between 1 and 1000");
        int deleted = 0;
        for (OwnedAudioAsset owned : repository.findExpired(clock.instant(), limit)) {
            try {
                storage.delete(owned.asset().objectKey());
                repository.markDeleted(owned.owner(), owned.asset().audioAssetId());
                deleted++;
            } catch (RuntimeException ignored) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "audio retention deletion failed for asset {0}; the next sweep will retry",
                        owned.asset().audioAssetId());
            }
        }
        return deleted;
    }
}
