package cn.forever24.tutor.resource;

import java.util.Set;

public record ImageAssetMetadata(
        String generationPrompt,
        AssetGenerationMetadata generation,
        Set<String> characterReferenceIds,
        String aspectRatio,
        ShotType shotType,
        Set<DisplaySurface> displaySurfaces,
        FocalPoint focalPoint,
        String altText,
        String sceneId,
        String continuityGroupId
) implements AssetMetadata {

    public ImageAssetMetadata {
        generationPrompt = ResourceValidation.required(generationPrompt, "generationPrompt");
        if (generationPrompt.length() < 20) {
            throw new IllegalArgumentException("generationPrompt must contain at least 20 characters");
        }
        if (generation == null) {
            throw new IllegalArgumentException("generation metadata is required");
        }
        if (characterReferenceIds == null || characterReferenceIds.isEmpty()) {
            throw new IllegalArgumentException("image character references are required");
        }
        characterReferenceIds = Set.copyOf(characterReferenceIds);
        aspectRatio = ResourceValidation.required(aspectRatio, "aspectRatio");
        if (!Set.of("16:9", "4:3", "1:1").contains(aspectRatio)) {
            throw new IllegalArgumentException("unsupported image aspect ratio: " + aspectRatio);
        }
        if (shotType == null || displaySurfaces == null || displaySurfaces.isEmpty() || focalPoint == null) {
            throw new IllegalArgumentException("image shot, display surfaces and focal point are required");
        }
        displaySurfaces = Set.copyOf(displaySurfaces);
        altText = ResourceValidation.required(altText, "altText");
        if (altText.length() < 10) {
            throw new IllegalArgumentException("altText must contain at least 10 characters");
        }
        sceneId = ResourceValidation.required(sceneId, "sceneId");
    }

    public boolean supportsTaskHeroPresentation() {
        return shotType != ShotType.PORTRAIT
                && displaySurfaces.contains(DisplaySurface.SCENARIO_INTRO)
                && displaySurfaces.contains(DisplaySurface.SCENARIO_TRAINING);
    }
}
