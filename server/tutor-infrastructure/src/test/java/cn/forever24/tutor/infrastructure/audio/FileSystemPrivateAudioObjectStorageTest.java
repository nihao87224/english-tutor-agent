package cn.forever24.tutor.infrastructure.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemPrivateAudioObjectStorageTest {
    @TempDir Path root;

    @Test
    void storesReadsDeletesAndRejectsPathEscape() {
        var storage = new FileSystemPrivateAudioObjectStorage(root);
        storage.put("user-recordings/usr-1/audio.webm", new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, storage.read("user-recordings/usr-1/audio.webm"));
        storage.delete("user-recordings/usr-1/audio.webm");
        assertThrows(IllegalArgumentException.class, () -> storage.put("../outside.webm", new byte[]{1}));
    }
}
