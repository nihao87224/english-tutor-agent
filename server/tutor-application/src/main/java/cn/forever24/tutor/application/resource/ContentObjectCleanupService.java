package cn.forever24.tutor.application.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/** Deletes only storage objects that have no live catalog reference; staging objects are always disposable. */
public final class ContentObjectCleanupService {
    private final ContentObjectStorage storage;
    public ContentObjectCleanupService(ContentObjectStorage storage) { this.storage = Objects.requireNonNull(storage); }
    public int cleanupUnreferenced(Collection<String> referencedObjectKeys) {
        var referenced = new HashSet<>(referencedObjectKeys == null ? java.util.List.<String>of() : referencedObjectKeys);
        int removed = 0;
        for (String key : storage.listObjectKeys()) {
            if (key.startsWith("staging/") || !referenced.contains(key)) { storage.delete(key); removed++; }
        }
        return removed;
    }
}
