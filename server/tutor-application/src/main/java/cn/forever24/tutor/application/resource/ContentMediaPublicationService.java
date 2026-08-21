package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceAsset;
import java.util.*;

/** Stores every referenced object before metadata, compensating finalized objects when metadata fails. */
public final class ContentMediaPublicationService {
    private final ContentObjectStorage storage;
    private final ResourceCatalogRepository catalog;
    public ContentMediaPublicationService(ContentObjectStorage storage, ResourceCatalogRepository catalog) { this.storage = Objects.requireNonNull(storage); this.catalog = Objects.requireNonNull(catalog); }
    public ResourceVersionSaveResult publishDraft(ResourceCatalogEntry entry, Map<String, byte[]> contentByAssetKey) {
        List<ContentObjectStorage.StagedObject> staged = new ArrayList<>();
        List<String> finalized = new ArrayList<>();
        try {
            for (ResourceAsset asset : entry.assets()) { byte[] content = contentByAssetKey.get(asset.assetKey()); if (content == null) throw new IllegalArgumentException("missing content for " + asset.assetKey()); staged.add(storage.stage(asset.objectKey(), content, asset.contentHash())); }
            for (var value : staged) { storage.finalize(value); finalized.add(value.finalObjectKey()); }
            return catalog.saveExactVersion(entry);
        } catch (RuntimeException exception) {
            for (String key : finalized) try { storage.delete(key); } catch (RuntimeException ignored) { }
            for (var value : staged) try { storage.discard(value); } catch (RuntimeException ignored) { }
            throw exception;
        }
    }
}
