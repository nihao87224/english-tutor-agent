package cn.forever24.tutor.infrastructure.audio;

import cn.forever24.tutor.application.audio.AudioAssetRepository;
import cn.forever24.tutor.application.audio.AudioAssetStoreRecord;
import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.UserKey;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAudioAssetRepository implements AudioAssetRepository {
    private final Map<String, OwnedAsset> assets = new ConcurrentHashMap<>();
    private final Map<String, AudioAssetStoreRecord> idempotency = new ConcurrentHashMap<>();

    @Override
    public Optional<AudioAssetStoreRecord> findByIdempotencyKey(UserKey userKey, String idempotencyKey) {
        return Optional.ofNullable(idempotency.get(userKey.value() + "|" + idempotencyKey));
    }

    @Override
    public Optional<UserAudioAsset> findById(UserKey userKey, String audioAssetId) {
        OwnedAsset value = assets.get(audioAssetId);
        return value != null && value.owner().equals(userKey) ? Optional.of(value.asset()) : Optional.empty();
    }

    @Override
    public synchronized void insert(UserKey userKey, UserAudioAsset asset, String idempotencyKey, String requestHash) {
        assets.put(asset.audioAssetId(), new OwnedAsset(userKey, asset));
        idempotency.put(userKey.value() + "|" + idempotencyKey, new AudioAssetStoreRecord(requestHash, asset));
    }

    @Override
    public void markDeleted(UserKey userKey, String audioAssetId) {
        findById(userKey, audioAssetId).ifPresent(asset -> assets.put(audioAssetId,
                new OwnedAsset(userKey, new UserAudioAsset(
                        asset.audioAssetId(), asset.objectKey(), asset.purpose(), asset.mimeType(), asset.byteLength(),
                        asset.durationMs(), asset.contentHash(), AudioAssetStatus.DELETED, asset.retention(),
                        asset.deleteAfter(), asset.createdAt()))));
    }

    @Override
    public List<cn.forever24.tutor.application.audio.OwnedAudioAsset> findExpired(Instant now, int limit) {
        return assets.values().stream()
                .filter(value -> value.asset().ready() && value.asset().deleteAfter() != null
                        && !value.asset().deleteAfter().isAfter(now))
                .limit(limit)
                .map(value -> new cn.forever24.tutor.application.audio.OwnedAudioAsset(value.owner(), value.asset()))
                .toList();
    }

    private record OwnedAsset(UserKey owner, UserAudioAsset asset) { }
}
