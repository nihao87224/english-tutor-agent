package cn.forever24.tutor.api.resource;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record CatalogResourceDetailResponse(
        String resourceId,
        String resourceVersion,
        String collectionId,
        String providerCode,
        String type,
        String title,
        String description,
        String language,
        String level,
        String topic,
        String scene,
        String communicationGoal,
        String accessScope,
        String publishStatus,
        int estimatedMinutes,
        Set<String> skillUnitVariantIds,
        CatalogAssetResponse taskHero,
        List<CatalogAssetResponse> audioAssets,
        JsonNode learnerFit,
        JsonNode content,
        List<CatalogAssetResponse> assets,
        Instant publishedAt
) {
    public CatalogResourceDetailResponse {
        skillUnitVariantIds = Set.copyOf(skillUnitVariantIds);
        audioAssets = List.copyOf(audioAssets);
        assets = List.copyOf(assets);
    }
}
