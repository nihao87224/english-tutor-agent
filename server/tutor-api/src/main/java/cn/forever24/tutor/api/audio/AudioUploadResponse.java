package cn.forever24.tutor.api.audio;

import cn.forever24.tutor.application.audio.AudioUploadResult;

public record AudioUploadResponse(
        String audioAssetId,
        String uploadStatus,
        String mimeType,
        long durationMs,
        String contentHash
) {
    static AudioUploadResponse from(AudioUploadResult result) {
        var asset = result.asset();
        return new AudioUploadResponse(asset.audioAssetId(), asset.status().name(), asset.mimeType(),
                asset.durationMs(), asset.contentHash());
    }
}
