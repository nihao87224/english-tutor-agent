package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacMediaAccessUrlIssuerTest {

    @Test
    void privateUrlIsActorScopedAndDoesNotExposePrivateObjectKey() {
        var asset = ResourceCatalogTestFixture.publishedEntry().assets().getFirst();
        var issuer = new HmacMediaAccessUrlIssuer(
                URI.create("https://cdn.example/content"),
                URI.create("https://media.example/access"),
                "test-signing-secret-with-at-least-32-characters");
        Instant expiry = Instant.parse("2026-08-20T00:10:00Z");

        var first = issuer.issuePrivate(
                new UserKey("usr_one"), "resource-one", asset, "idem-12345678", expiry);
        var second = issuer.issuePrivate(
                new UserKey("usr_two"), "resource-one", asset, "idem-12345678", expiry);

        assertEquals(expiry, first.expiresAt());
        assertTrue(first.url().toString().startsWith("https://media.example/access/"));
        assertFalse(first.url().toString().contains(asset.objectKey()));
        assertNotEquals(first.url(), second.url());
    }

    @Test
    void publicUrlUsesCdnAndHasNoExpiry() {
        var asset = ResourceCatalogTestFixture.publishedEntry().assets().getFirst();
        var issuer = new HmacMediaAccessUrlIssuer(
                URI.create("https://cdn.example/content"),
                URI.create("https://media.example/access"),
                "test-signing-secret-with-at-least-32-characters");

        var access = issuer.publicUrl(asset);

        assertTrue(access.url().toString().startsWith("https://cdn.example/content/images/"));
        assertEquals(null, access.expiresAt());
    }

    @Test
    void revokedPrivateGrantCannotPassGatewayVerification() {
        var asset = ResourceCatalogTestFixture.publishedEntry().assets().getFirst();
        var issuer = new HmacMediaAccessUrlIssuer(URI.create("https://cdn.example/content"), URI.create("https://media.example/access"),
                "test-signing-secret-with-at-least-32-characters");
        var user = new UserKey("usr_one");
        Instant expiry = Instant.parse("2026-08-20T00:10:00Z");
        var grant = issuer.issuePrivate(user, "resource-one", asset, "idem", expiry);
        String signature = java.net.URLDecoder.decode(grant.url().getQuery().replaceFirst(".*signature=", ""), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(issuer.verifiesPrivateGrant(user, asset.assetKey(), expiry, 0, signature, Instant.parse("2026-08-20T00:00:00Z")));
        issuer.revokePrivate(asset.assetKey());
        assertFalse(issuer.verifiesPrivateGrant(user, asset.assetKey(), expiry, 0, signature, Instant.parse("2026-08-20T00:00:00Z")));
    }
}
