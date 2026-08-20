package cn.forever24.tutor.resource;

import cn.forever24.tutor.curriculum.CefrLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceCatalogEntryTest {

    @Test
    void acceptsPublishedScenarioWithOneEnvironmentalTaskHero() {
        ResourceCatalogEntry entry = entry(
                List.of(taskHero("hero", validHeroMetadata())),
                List.of(new AssetReference("hero", 0)),
                ContentProviderType.INTERNAL,
                null);

        assertEquals("season1.ep006.gate_change.b1", entry.resource().resourceKey());
    }

    @Test
    void rejectsMissingOrDuplicateTaskHero() {
        ResourceAsset audio = audio("dialogue");
        assertThrows(IllegalArgumentException.class, () -> entry(
                List.of(audio),
                List.of(new AssetReference("dialogue", 0)),
                ContentProviderType.INTERNAL,
                null));

        assertThrows(IllegalArgumentException.class, () -> entry(
                List.of(taskHero("hero-a", validHeroMetadata()), taskHero("hero-b", validHeroMetadata())),
                List.of(new AssetReference("hero-a", 0), new AssetReference("hero-b", 1)),
                ContentProviderType.INTERNAL,
                null));
    }

    @Test
    void rejectsPortraitOrMissingTrainingSurfaceForTaskHero() {
        ImageAssetMetadata invalidMetadata = new ImageAssetMetadata(
                "Generate Lin Muen in a clear airport scene for an English lesson.",
                generation(),
                Set.of("lin-muen-main-v1"),
                "16:9",
                ShotType.PORTRAIT,
                Set.of(DisplaySurface.SCENARIO_INTRO),
                new FocalPoint(0.6, 0.5),
                "Lin Muen waits near the changed airport boarding gate.",
                "GATE_CHANGE",
                "season1.ep006");

        assertThrows(IllegalArgumentException.class, () -> entry(
                List.of(taskHero("hero", invalidMetadata)),
                List.of(new AssetReference("hero", 0)),
                ContentProviderType.INTERNAL,
                null));
    }

    @Test
    void rejectsUnknownAssetReference() {
        assertThrows(IllegalArgumentException.class, () -> entry(
                List.of(taskHero("hero", validHeroMetadata())),
                List.of(new AssetReference("missing", 0)),
                ContentProviderType.INTERNAL,
                null));
    }

    @Test
    void requiresLicenseMetadataForThirdPartyCollection() {
        assertThrows(IllegalArgumentException.class, () -> entry(
                List.of(taskHero("hero", validHeroMetadata())),
                List.of(new AssetReference("hero", 0)),
                ContentProviderType.THIRD_PARTY,
                null));
    }

    private static ResourceCatalogEntry entry(
            List<ResourceAsset> assets,
            List<AssetReference> references,
            ContentProviderType providerType,
            String licenseNote
    ) {
        Instant now = Instant.parse("2026-08-20T00:00:00Z");
        ContentProvider provider = new ContentProvider("english-tutor-agent", "English Tutor", providerType);
        ResourceCollection collection = new ResourceCollection(
                "INTERNAL_SCENARIO_LIBRARY",
                provider.providerCode(),
                "Internal Scenario Library",
                AccessScope.PUBLIC,
                CollectionStatus.ACTIVE,
                null,
                providerType == ContentProviderType.INTERNAL ? "OWNED" : "LICENSED",
                licenseNote,
                "LEARNER",
                null);
        LearningResource resource = new LearningResource(
                "season1.ep006.gate_change.b1",
                provider.providerCode(),
                collection.collectionKey(),
                ResourceType.SCENARIO_LESSON,
                "Confirm a Gate Change with Lin Muen",
                "A scenario lesson at an airport gate.",
                "en",
                CefrLevel.B1,
                "Travel",
                "GATE_CHANGE",
                "Confirm changed travel information",
                AccessScope.PUBLIC,
                PublishStatus.PUBLISHED,
                "1.0.0",
                12);
        ResourceVersion version = new ResourceVersion(
                resource.resourceKey(),
                "1.0.0",
                hash('a'),
                "{}",
                "{}",
                "{}",
                Set.of("travel.confirm_gate_change.b1"),
                references,
                ResourceVersionStatus.PUBLISHED,
                now,
                now);
        return new ResourceCatalogEntry(provider, collection, resource, version, assets);
    }

    private static ResourceAsset taskHero(String assetKey, ImageAssetMetadata metadata) {
        return new ResourceAsset(
                assetKey,
                "1.0.0",
                AssetMediaType.IMAGE,
                AssetPurpose.TASK_HERO,
                "images/season1/ep006/" + assetKey + ".webp",
                hash(assetKey.endsWith("b") ? 'b' : assetKey.endsWith("a") ? 'a' : 'c'),
                "image/webp",
                1024,
                AccessScope.PUBLIC,
                metadata,
                AssetStatus.ACTIVE,
                Instant.parse("2026-08-20T00:00:00Z"));
    }

    private static ResourceAsset audio(String assetKey) {
        return new ResourceAsset(
                assetKey,
                "1.0.0",
                AssetMediaType.AUDIO,
                AssetPurpose.SCENE_DIALOGUE,
                "audio/season1/ep006/" + assetKey + ".mp3",
                hash('b'),
                "audio/mpeg",
                2048,
                AccessScope.PUBLIC,
                new AudioAssetMetadata(
                        "Generate a natural airport dialogue for a B1 English lesson.",
                        generation(),
                        "Lin Muen and Airport Agent",
                        "lin-muen-v1+airport-agent-v1",
                        "American neutral",
                        0.95,
                        "gate-change-transcript",
                        List.of(new AudioScriptLine("gate-001", "Lin Muen", "Could you help me check?"))),
                AssetStatus.ACTIVE,
                Instant.parse("2026-08-20T00:00:00Z"));
    }

    private static ImageAssetMetadata validHeroMetadata() {
        return new ImageAssetMetadata(
                "Generate Lin Muen full-body at an airport gate confirming changed flight information.",
                generation(),
                Set.of("lin-muen-main-v1", "lin-muen-travel-outfit-v1"),
                "16:9",
                ShotType.ENVIRONMENTAL_FULL_BODY,
                Set.of(
                        DisplaySurface.PRESCRIPTION_CARD,
                        DisplaySurface.SCENARIO_INTRO,
                        DisplaySurface.SCENARIO_TRAINING),
                new FocalPoint(0.62, 0.48),
                "Lin Muen stands near an airport gate and checks the changed flight details.",
                "GATE_CHANGE",
                "season1.ep006");
    }

    private static AssetGenerationMetadata generation() {
        return new AssetGenerationMetadata("openai", "media-model", "2026-08", "1.0.0");
    }

    private static String hash(char value) {
        return "sha256:" + String.valueOf(value).repeat(64);
    }
}
