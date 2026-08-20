package cn.forever24.tutor.resource;

public sealed interface AssetMetadata permits ImageAssetMetadata, AudioAssetMetadata {

    String generationPrompt();

    AssetGenerationMetadata generation();
}
