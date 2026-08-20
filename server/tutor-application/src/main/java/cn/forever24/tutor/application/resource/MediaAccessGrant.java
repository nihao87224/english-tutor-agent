package cn.forever24.tutor.application.resource;

import java.net.URI;
import java.time.Instant;

public record MediaAccessGrant(URI url, Instant expiresAt) {
    public MediaAccessGrant {
        if (url == null) {
            throw new IllegalArgumentException("media access url is required");
        }
    }
}
