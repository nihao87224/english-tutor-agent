package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetGenerationMetadata;
import cn.forever24.tutor.resource.AssetMediaType;
import cn.forever24.tutor.resource.AssetPurpose;
import cn.forever24.tutor.resource.AssetReference;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.AudioAssetMetadata;
import cn.forever24.tutor.resource.AudioScriptLine;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.ContentProvider;
import cn.forever24.tutor.resource.ContentProviderType;
import cn.forever24.tutor.resource.DisplaySurface;
import cn.forever24.tutor.resource.FocalPoint;
import cn.forever24.tutor.resource.ImageAssetMetadata;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import cn.forever24.tutor.resource.ResourceVersion;
import cn.forever24.tutor.resource.ResourceVersionStatus;
import cn.forever24.tutor.resource.ShotType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

final class ResourceCatalogTestFixture {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    private ResourceCatalogTestFixture() {
    }

    static ResourceCatalogEntry publishedEntry() {
        return entry("", 'a', PublishStatus.PUBLISHED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE);
    }

    static ResourceCatalogEntry entry(
            String suffix,
            char manifestHash,
            PublishStatus publishStatus,
            CollectionStatus collectionStatus,
            AssetStatus assetStatus
    ) {
        String resourceKey = "season1.ep006.gate_change.b1" + suffix;
        String heroKey = resourceKey + ".task-hero";
        String audioKey = resourceKey + ".scene-dialogue";
        ContentProvider provider = new ContentProvider(
                "english-tutor-agent" + suffix,
                "English Tutor Agent" + suffix,
                ContentProviderType.INTERNAL);
        ResourceCollection collection = new ResourceCollection(
                "INTERNAL_SCENARIO_LIBRARY" + suffix.toUpperCase(),
                provider.providerCode(),
                "Internal Scenario Library" + suffix,
                AccessScope.PUBLIC,
                collectionStatus,
                null,
                "OWNED",
                "Internal product content",
                "LEARNER",
                null);
        LearningResource resource = new LearningResource(
                resourceKey,
                provider.providerCode(),
                collection.collectionKey(),
                ResourceType.SCENARIO_LESSON,
                "Confirm a Gate Change with Lin Muen" + suffix,
                "Help Lin Muen confirm changed flight information.",
                "en",
                CefrLevel.B1,
                "Travel",
                "GATE_CHANGE",
                "Confirm changed travel information and ask what happens next",
                AccessScope.PUBLIC,
                publishStatus,
                "1.0.0",
                12);
        ResourceVersion version = new ResourceVersion(
                resourceKey,
                "1.0.0",
                hash(manifestHash),
                "{\"schemaVersion\":\"2.0.1\"}",
                "{\"goalTags\":[\"travel_communication\"]}",
                "{\"promptVersion\":\"1.0.0\"}",
                Set.of("travel.confirm_gate_change.b1"),
                List.of(new AssetReference(heroKey, 0), new AssetReference(audioKey, 1)),
                ResourceVersionStatus.PUBLISHED,
                NOW,
                NOW);
        return new ResourceCatalogEntry(
                provider,
                collection,
                resource,
                version,
                List.of(taskHero(heroKey, assetStatus), audio(audioKey, assetStatus)));
    }

    private static ResourceAsset taskHero(String assetKey, AssetStatus status) {
        return new ResourceAsset(
                assetKey,
                "1.0.0",
                AssetMediaType.IMAGE,
                AssetPurpose.TASK_HERO,
                "images/season1/ep006/gate_change/b1/" + safeName(assetKey) + ".webp",
                hash('b'),
                "image/webp",
                4096,
                AccessScope.PUBLIC,
                new ImageAssetMetadata(
                        "Generate Lin Muen full-body at an airport gate confirming changed flight information.",
                        generation("image-model"),
                        Set.of("lin-muen-main-v1", "lin-muen-travel-outfit-v1"),
                        "16:9",
                        ShotType.ENVIRONMENTAL_FULL_BODY,
                        Set.of(
                                DisplaySurface.PRESCRIPTION_CARD,
                                DisplaySurface.SCENARIO_INTRO,
                                DisplaySurface.SCENARIO_TRAINING),
                        new FocalPoint(0.62, 0.48),
                        "Lin Muen stands near an airport boarding gate and checks changed flight details.",
                        "GATE_CHANGE",
                        "season1.ep006.gate-change"),
                status,
                NOW);
    }

    private static ResourceAsset audio(String assetKey, AssetStatus status) {
        return new ResourceAsset(
                assetKey,
                "1.0.0",
                AssetMediaType.AUDIO,
                AssetPurpose.SCENE_DIALOGUE,
                "audio/season1/ep006/gate_change/b1/" + safeName(assetKey) + ".mp3",
                hash('c'),
                "audio/mpeg",
                8192,
                AccessScope.PUBLIC,
                new AudioAssetMetadata(
                        "Generate a natural B1 airport dialogue with two distinct voices and realistic pacing.",
                        generation("tts-model"),
                        "Lin Muen and Airport Agent",
                        "lin-muen-v1+airport-agent-v1",
                        "American neutral",
                        0.95,
                        "season1.ep006.gate_change.b1.transcript",
                        List.of(
                                new AudioScriptLine(
                                        "gate-001",
                                        "Lin Muen",
                                        "Could you help me check the new boarding gate?"),
                                new AudioScriptLine(
                                        "gate-002",
                                        "Airport Agent",
                                        "Your flight now departs from Gate 24."))),
                status,
                NOW);
    }

    private static AssetGenerationMetadata generation(String model) {
        return new AssetGenerationMetadata("openai", model, "2026-08", "1.0.0");
    }

    private static String safeName(String value) {
        return value.replace('.', '-');
    }

    private static String hash(char value) {
        return "sha256:" + String.valueOf(value).repeat(64);
    }
}
