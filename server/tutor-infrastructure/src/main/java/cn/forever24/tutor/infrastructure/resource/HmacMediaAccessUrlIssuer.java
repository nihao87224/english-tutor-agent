package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.MediaAccessGrant;
import cn.forever24.tutor.application.resource.MediaAccessUrlIssuer;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceAsset;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HmacMediaAccessUrlIssuer implements MediaAccessUrlIssuer {

    private final URI publicBaseUrl;
    private final URI privateGatewayBaseUrl;
    private final byte[] signingSecret;
    private final Map<String, Long> revocationVersions = new ConcurrentHashMap<>();

    public HmacMediaAccessUrlIssuer(URI publicBaseUrl, URI privateGatewayBaseUrl, String signingSecret) {
        this.publicBaseUrl = requireBaseUrl(publicBaseUrl, "publicBaseUrl");
        this.privateGatewayBaseUrl = requireBaseUrl(privateGatewayBaseUrl, "privateGatewayBaseUrl");
        if (signingSecret == null || signingSecret.length() < 32) {
            throw new IllegalArgumentException("media access signing secret must contain at least 32 characters");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public MediaAccessGrant publicUrl(ResourceAsset asset) {
        return new MediaAccessGrant(resolve(publicBaseUrl, asset.objectKey()), null);
    }

    @Override
    public MediaAccessGrant issuePrivate(
            UserKey userKey,
            String resourceKey,
            ResourceAsset asset,
            String idempotencyKey,
            Instant expiresAt
    ) {
        String expires = Long.toString(expiresAt.getEpochSecond());
        long version = revocationVersions.getOrDefault(asset.assetKey(), 0L);
        String payload = userKey.value() + "\n" + asset.assetKey() + "\n" + expires + "\n" + version;
        String signature = sign(payload);
        String opaqueAsset = encode(asset.assetKey());
        URI url = URI.create(trimTrailingSlash(privateGatewayBaseUrl.toString()) + "/" + opaqueAsset
                + "?expires=" + expires + "&version=" + version + "&signature=" + encode(signature));
        return new MediaAccessGrant(url, expiresAt);
    }

    @Override
    public void revokePrivate(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) throw new IllegalArgumentException("assetKey is required");
        revocationVersions.merge(assetKey.strip(), 1L, Long::sum);
    }

    /** Gateway-facing verification rejects expired, altered, owner-mismatched and revoked grants. */
    public boolean verifiesPrivateGrant(UserKey userKey, String assetKey, Instant expiresAt, long version, String signature, Instant now) {
        if (userKey == null || assetKey == null || expiresAt == null || signature == null || now == null || !now.isBefore(expiresAt)) return false;
        if (version != revocationVersions.getOrDefault(assetKey, 0L)) return false;
        String payload = userKey.value() + "\n" + assetKey + "\n" + expiresAt.getEpochSecond() + "\n" + version;
        return java.security.MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("media access url could not be signed", exception);
        }
    }

    private static URI resolve(URI baseUrl, String objectKey) {
        String[] segments = objectKey.split("/");
        StringBuilder encodedPath = new StringBuilder();
        for (String segment : segments) {
            if (!encodedPath.isEmpty()) {
                encodedPath.append('/');
            }
            encodedPath.append(encode(segment));
        }
        return URI.create(trimTrailingSlash(baseUrl.toString()) + "/" + encodedPath);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static URI requireBaseUrl(URI value, String field) {
        if (value == null || !value.isAbsolute()) {
            throw new IllegalArgumentException(field + " must be absolute");
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
