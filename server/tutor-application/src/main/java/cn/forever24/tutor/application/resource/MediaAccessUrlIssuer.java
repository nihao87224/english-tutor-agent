package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceAsset;

import java.time.Instant;

public interface MediaAccessUrlIssuer {

    MediaAccessGrant publicUrl(ResourceAsset asset);

    MediaAccessGrant issuePrivate(
            UserKey userKey,
            String resourceKey,
            ResourceAsset asset,
            String idempotencyKey,
            Instant expiresAt
    );

    /** Invalidates previously issued private grants for this asset. */
    default void revokePrivate(String assetKey) {
        throw new UnsupportedOperationException("private media revocation is not supported");
    }
}
