package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceAsset;

import java.util.Optional;

public interface AssetMetadataRepository {

    Optional<ResourceAsset> findAsset(String assetKey);
}
