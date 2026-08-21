package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.ContentObjectStorage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.UUID;

public final class FileSystemContentObjectStorage implements ContentObjectStorage {
    private final Path root;
    public FileSystemContentObjectStorage(Path root) { this.root = root.toAbsolutePath().normalize(); }
    @Override public StagedObject stage(String finalKey, byte[] content, String expectedHash) {
        String actual = hash(content); if (!actual.equals(expectedHash)) throw new IllegalArgumentException("content hash mismatch");
        String staging = "staging/" + UUID.randomUUID() + "/" + finalKey;
        try { Path target = resolve(staging); Files.createDirectories(target.getParent()); Files.write(target, content, StandardOpenOption.CREATE_NEW); return new StagedObject(staging, finalKey, actual); }
        catch (Exception exception) { throw new IllegalStateException("content object could not be staged", exception); }
    }
    @Override public void finalize(StagedObject staged) { try { Path source = resolve(staged.stagingKey()); Path target = resolve(staged.finalObjectKey()); Files.createDirectories(target.getParent()); Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); } catch (AtomicMoveNotSupportedException e) { try { Files.move(resolve(staged.stagingKey()), resolve(staged.finalObjectKey())); } catch (Exception x) { throw new IllegalStateException("content object could not be finalized", x); } } catch (Exception e) { throw new IllegalStateException("content object could not be finalized", e); } }
    @Override public void discard(StagedObject staged) { try { Files.deleteIfExists(resolve(staged.stagingKey())); } catch (Exception e) { throw new IllegalStateException("staged object could not be discarded", e); } }
    @Override public void delete(String key) { try { Files.deleteIfExists(resolve(key)); } catch (Exception e) { throw new IllegalStateException("content object could not be deleted", e); } }
    @Override public boolean exists(String key) { return Files.isRegularFile(resolve(key)); }
    @Override public java.util.List<String> listObjectKeys() { try (var paths = Files.exists(root) ? Files.walk(root) : java.util.stream.Stream.<Path>empty()) { return paths.filter(Files::isRegularFile).map(root::relativize).map(Path::toString).map(value -> value.replace('\\', '/')).toList(); } catch (Exception e) { throw new IllegalStateException("content objects could not be listed", e); } }
    private Path resolve(String key) { Path value = root.resolve(key).normalize(); if (!value.startsWith(root)) throw new IllegalArgumentException("object key escapes storage root"); return value; }
    private static String hash(byte[] value) { try { return "sha256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (Exception e) { throw new IllegalStateException(e); } }
}
