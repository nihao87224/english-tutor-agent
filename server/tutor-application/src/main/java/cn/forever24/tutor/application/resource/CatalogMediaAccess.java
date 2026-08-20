package cn.forever24.tutor.application.resource;

import java.net.URI;
import java.time.Instant;

public record CatalogMediaAccess(
        String assetKey,
        URI url,
        Instant expiresAt,
        String mimeType,
        String contentHash
) {
}
