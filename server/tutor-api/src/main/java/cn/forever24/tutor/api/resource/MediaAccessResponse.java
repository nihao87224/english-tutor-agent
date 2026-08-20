package cn.forever24.tutor.api.resource;

import java.time.Instant;

public record MediaAccessResponse(
        String assetId,
        String url,
        Instant expiresAt,
        String mimeType,
        String contentHash
) {
}
