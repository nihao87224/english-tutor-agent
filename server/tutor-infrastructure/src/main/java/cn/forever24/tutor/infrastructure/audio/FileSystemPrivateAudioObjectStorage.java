package cn.forever24.tutor.infrastructure.audio;

import cn.forever24.tutor.application.audio.PrivateAudioObjectStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

public final class FileSystemPrivateAudioObjectStorage implements PrivateAudioObjectStorage {
    private final Path root;

    public FileSystemPrivateAudioObjectStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void put(String objectKey, byte[] content) {
        Path target = resolve(objectKey);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new IllegalStateException("private audio object could not be stored", exception);
        }
    }

    @Override
    public byte[] read(String objectKey) {
        try { return Files.readAllBytes(resolve(objectKey)); }
        catch (IOException exception) { throw new IllegalStateException("private audio object could not be read", exception); }
    }

    @Override
    public void delete(String objectKey) {
        try { Files.deleteIfExists(resolve(objectKey)); }
        catch (IOException exception) { throw new IllegalStateException("private audio object could not be deleted", exception); }
    }

    private Path resolve(String objectKey) {
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("object key escapes private storage root");
        return target;
    }
}
