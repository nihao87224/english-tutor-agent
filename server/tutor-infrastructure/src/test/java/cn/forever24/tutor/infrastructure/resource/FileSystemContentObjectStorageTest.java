package cn.forever24.tutor.infrastructure.resource;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
import cn.forever24.tutor.application.resource.ContentObjectCleanupService;
class FileSystemContentObjectStorageTest {
 @Test void stagesVerifiesAndFinalizes() throws Exception { var storage = new FileSystemContentObjectStorage(Files.createTempDirectory("content-storage")); byte[] content = "asset".getBytes(); var staged = storage.stage("images/a.webp", content, "sha256:d59386e0ae435e292fbe0ebcdb954b75ed5fb3922091277cb19f798fc5d50718"); storage.finalize(staged); assertTrue(storage.exists("images/a.webp")); assertFalse(storage.exists(staged.stagingKey())); assertThrows(IllegalArgumentException.class, () -> storage.stage("images/b.webp", content, "sha256:" + "0".repeat(64))); }
 @Test void cleanupKeepsCatalogReferencedObjects() throws Exception { var storage = new FileSystemContentObjectStorage(Files.createTempDirectory("content-cleanup")); byte[] content = "asset".getBytes(); String hash = "sha256:d59386e0ae435e292fbe0ebcdb954b75ed5fb3922091277cb19f798fc5d50718"; storage.finalize(storage.stage("images/keep.webp", content, hash)); storage.finalize(storage.stage("images/orphan.webp", content, hash)); assertEquals(1, new ContentObjectCleanupService(storage).cleanupUnreferenced(java.util.List.of("images/keep.webp"))); assertTrue(storage.exists("images/keep.webp")); assertFalse(storage.exists("images/orphan.webp")); }
}
