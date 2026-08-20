package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.resource.AssetMediaType;
import cn.forever24.tutor.resource.AudioAssetMetadata;
import cn.forever24.tutor.resource.ImageAssetMetadata;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CatalogResponseMapper {

    private static final Set<String> PRIVATE_FIELDS = Set.of(
            "objectKey", "generationPrompt", "generationMetadata", "licenseNote", "adminNote");

    private final ObjectMapper objectMapper;

    CatalogResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    CatalogResourceSummaryResponse summary(PublishedResourceCandidate candidate) {
        List<CatalogAssetResponse> audio = candidate.assets().stream()
                .filter(asset -> asset.mediaType() == AssetMediaType.AUDIO)
                .map(this::asset)
                .toList();
        return new CatalogResourceSummaryResponse(
                candidate.resourceKey(), candidate.semanticVersion(), candidate.collectionKey(),
                candidate.resourceType().name(), candidate.title(), candidate.level().name(),
                candidate.topic(), candidate.scene(), candidate.communicationGoal(),
                candidate.accessScope().name(), "PUBLISHED", candidate.estimatedMinutes(),
                candidate.skillUnitVariantKeys(), asset(candidate.taskHero()), audio);
    }

    CatalogResourceSummaryResponse summary(ResourceCatalogEntry entry) {
        ResourceAsset taskHero = entry.assets().stream()
                .filter(asset -> asset.purpose().name().equals("TASK_HERO"))
                .findFirst()
                .orElse(null);
        return new CatalogResourceSummaryResponse(
                entry.resource().resourceKey(), entry.resourceVersion().semanticVersion(),
                entry.resource().collectionKey(), entry.resource().type().name(), entry.resource().title(),
                entry.resource().level().name(), entry.resource().topic(), entry.resource().scene(),
                entry.resource().communicationGoal(), entry.resource().accessScope().name(),
                entry.resource().publishStatus().name(), entry.resource().estimatedMinutes(),
                entry.resourceVersion().skillUnitVariantKeys(),
                taskHero == null ? null : asset(taskHero),
                entry.assets().stream().filter(item -> item.mediaType() == AssetMediaType.AUDIO)
                        .map(this::asset).toList());
    }

    CatalogResourceDetailResponse detail(ResourceCatalogEntry entry) {
        ResourceAsset taskHero = entry.assets().stream()
                .filter(asset -> asset.purpose().name().equals("TASK_HERO"))
                .findFirst()
                .orElse(null);
        List<CatalogAssetResponse> audioAssets = entry.assets().stream()
                .filter(asset -> asset.mediaType() == AssetMediaType.AUDIO)
                .map(this::asset)
                .toList();
        return new CatalogResourceDetailResponse(
                entry.resource().resourceKey(), entry.resourceVersion().semanticVersion(),
                entry.resource().collectionKey(), entry.resource().providerCode(),
                entry.resource().type().name(), entry.resource().title(), entry.resource().description(),
                entry.resource().language(), entry.resource().level().name(), entry.resource().topic(),
                entry.resource().scene(), entry.resource().communicationGoal(),
                entry.resource().accessScope().name(), entry.resource().publishStatus().name(),
                entry.resource().estimatedMinutes(), entry.resourceVersion().skillUnitVariantKeys(),
                taskHero == null ? null : asset(taskHero), audioAssets,
                safeJson(entry.resourceVersion().learnerFitJson()),
                safeJson(entry.resourceVersion().manifestJson()),
                entry.assets().stream().map(this::asset).toList(),
                entry.resourceVersion().publishedAt());
    }

    private CatalogAssetResponse asset(ResourceAsset asset) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (asset.metadata() instanceof ImageAssetMetadata image) {
            metadata.put("aspectRatio", image.aspectRatio());
            metadata.put("shotType", image.shotType().name());
            metadata.put("displaySurfaces", image.displaySurfaces().stream().map(Enum::name).toList());
            metadata.put("focalPoint", image.focalPoint());
            metadata.put("altText", image.altText());
            metadata.put("sceneId", image.sceneId());
            metadata.put("continuityGroupId", image.continuityGroupId());
        } else if (asset.metadata() instanceof AudioAssetMetadata audio) {
            metadata.put("speakerRole", audio.speakerRole());
            metadata.put("voiceId", audio.voiceId());
            metadata.put("accent", audio.accent());
            metadata.put("speechRate", audio.speechRate());
            metadata.put("transcriptRef", audio.transcriptRef());
            metadata.put("audioScript", audio.audioScript());
        }
        metadata.values().removeIf(java.util.Objects::isNull);
        return new CatalogAssetResponse(
                asset.assetKey(), asset.assetVersion(), asset.mediaType().name(), asset.purpose().name(),
                asset.accessScope().name(), asset.mimeType(), asset.contentHash(), asset.byteLength(), metadata);
    }

    private JsonNode safeJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            removePrivateFields(node);
            return node;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored catalog JSON is invalid", exception);
        }
    }

    private static void removePrivateFields(JsonNode node) {
        if (node instanceof ObjectNode object) {
            PRIVATE_FIELDS.forEach(object::remove);
            object.elements().forEachRemaining(CatalogResponseMapper::removePrivateFields);
        } else if (node.isArray()) {
            node.elements().forEachRemaining(CatalogResponseMapper::removePrivateFields);
        }
    }
}
