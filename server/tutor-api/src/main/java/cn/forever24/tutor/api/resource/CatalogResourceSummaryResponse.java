package cn.forever24.tutor.api.resource;

import java.util.List;
import java.util.Set;

public record CatalogResourceSummaryResponse(
        String resourceId,
        String resourceVersion,
        String collectionId,
        String type,
        String title,
        String level,
        String topic,
        String scene,
        String communicationGoal,
        String accessScope,
        String publishStatus,
        int estimatedMinutes,
        Set<String> skillUnitVariantIds,
        CatalogAssetResponse taskHero,
        List<CatalogAssetResponse> audioAssets
) {
    public CatalogResourceSummaryResponse {
        skillUnitVariantIds = Set.copyOf(skillUnitVariantIds);
        audioAssets = List.copyOf(audioAssets);
    }
}
