package cn.forever24.tutor.api.resource;

import java.util.Map;

public record CatalogAssetResponse(
        String assetId,
        String assetVersion,
        String mediaType,
        String purpose,
        String accessScope,
        String mimeType,
        String contentHash,
        long byteLength,
        Map<String, Object> metadata
) {
    public CatalogAssetResponse {
        metadata = Map.copyOf(metadata);
    }
}
