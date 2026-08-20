package cn.forever24.tutor.resource;

import java.time.Instant;
import java.util.Set;

public record ResourceAsset(
        String assetKey,
        String assetVersion,
        AssetMediaType mediaType,
        AssetPurpose purpose,
        String objectKey,
        String contentHash,
        String mimeType,
        long byteLength,
        AccessScope accessScope,
        AssetMetadata metadata,
        AssetStatus status,
        Instant createdAt
) {
    private static final Set<AssetPurpose> IMAGE_PURPOSES = Set.of(
            AssetPurpose.TASK_HERO, AssetPurpose.SCENE_STATE, AssetPurpose.CHARACTER_FALLBACK);
    private static final Set<AssetPurpose> AUDIO_PURPOSES = Set.of(
            AssetPurpose.SCENE_DIALOGUE, AssetPurpose.ROLE_PLAY_PROMPT, AssetPurpose.REVIEW_PROMPT);

    public ResourceAsset {
        assetKey = ResourceValidation.required(assetKey, "assetKey");
        assetVersion = ResourceValidation.semanticVersion(assetVersion);
        if (mediaType == null || purpose == null) {
            throw new IllegalArgumentException("asset media type and purpose are required");
        }
        objectKey = ResourceValidation.required(objectKey, "objectKey");
        contentHash = ResourceValidation.contentHash(contentHash);
        mimeType = ResourceValidation.required(mimeType, "mimeType");
        if (byteLength <= 0) {
            throw new IllegalArgumentException("asset byteLength must be positive");
        }
        if (accessScope == null || metadata == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("asset access, metadata, status and createdAt are required");
        }
        if (mediaType == AssetMediaType.IMAGE
                && (!(metadata instanceof ImageAssetMetadata) || !IMAGE_PURPOSES.contains(purpose))) {
            throw new IllegalArgumentException("image asset metadata or purpose is invalid");
        }
        if (mediaType == AssetMediaType.AUDIO
                && (!(metadata instanceof AudioAssetMetadata) || !AUDIO_PURPOSES.contains(purpose))) {
            throw new IllegalArgumentException("audio asset metadata or purpose is invalid");
        }
        if (mediaType == AssetMediaType.IMAGE && !objectKey.startsWith("images/")) {
            throw new IllegalArgumentException("image object key must start with images/");
        }
        if (mediaType == AssetMediaType.AUDIO && !objectKey.startsWith("audio/")) {
            throw new IllegalArgumentException("audio object key must start with audio/");
        }
    }
}
