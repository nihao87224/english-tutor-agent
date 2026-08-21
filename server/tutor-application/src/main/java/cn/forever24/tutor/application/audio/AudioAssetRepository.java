package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.UserKey;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

public interface AudioAssetRepository {
    Optional<AudioAssetStoreRecord> findByIdempotencyKey(UserKey userKey, String idempotencyKey);
    Optional<UserAudioAsset> findById(UserKey userKey, String audioAssetId);
    void insert(UserKey userKey, UserAudioAsset asset, String idempotencyKey, String requestHash);
    void markDeleted(UserKey userKey, String audioAssetId);
    List<OwnedAudioAsset> findExpired(Instant now, int limit);
}
