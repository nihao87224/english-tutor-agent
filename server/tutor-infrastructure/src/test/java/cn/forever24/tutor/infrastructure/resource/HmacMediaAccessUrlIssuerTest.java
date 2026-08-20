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
}
