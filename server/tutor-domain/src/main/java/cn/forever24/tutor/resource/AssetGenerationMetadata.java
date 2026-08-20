package cn.forever24.tutor.resource;

public record AssetGenerationMetadata(
        String provider,
        String model,
        String modelVersion,
        String promptVersion
) {
    public AssetGenerationMetadata {
        provider = ResourceValidation.required(provider, "generation.provider");
        model = ResourceValidation.required(model, "generation.model");
        modelVersion = ResourceValidation.required(modelVersion, "generation.modelVersion");
        promptVersion = ResourceValidation.semanticVersion(promptVersion);
    }
}
