package cn.forever24.tutor.audio;

import cn.forever24.tutor.profile.RawContentRetention;

import java.time.Instant;

public record UserAudioAsset(
        String audioAssetId,
        String objectKey,
        String purpose,
        String mimeType,
        long byteLength,
        long durationMs,
        String contentHash,
        AudioAssetStatus status,
        RawContentRetention retention,
        Instant deleteAfter,
        Instant createdAt
) {
    public UserAudioAsset {
        requireText(audioAssetId, "audioAssetId");
        requireText(objectKey, "objectKey");
        requireText(purpose, "purpose");
        requireText(mimeType, "mimeType");
        requireText(contentHash, "contentHash");
        if (byteLength < 1 || durationMs < 1) {
            throw new IllegalArgumentException("audio byteLength and durationMs must be positive");
        }
        if (status == null || retention == null || createdAt == null) {
            throw new IllegalArgumentException("audio status, retention and createdAt are required");
        }
    }

    public boolean ready() {
        return status == AudioAssetStatus.READY;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
