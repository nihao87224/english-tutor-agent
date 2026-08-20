package cn.forever24.tutor.application.resource;

public record ResourceVersionSaveResult(
        String resourceKey,
        String semanticVersion,
        String manifestHash,
        CatalogWriteOutcome outcome
) {
}
