package cn.forever24.tutor.infrastructure.content;

import cn.forever24.tutor.application.content.ContentManifestValidator;
import cn.forever24.tutor.content.ContentImportIssue;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Strict, dependency-free runtime counterpart to the checked-in V2 JSON Schema contracts. */
public final class JacksonContentManifestValidator implements ContentManifestValidator {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "resourceId", "resourceVersion", "provider",
            "collectionId", "resourceType", "title", "level", "topic", "accessScope", "skillUnitVariants",
            "episodeMappings", "lessonPackage", "assets");
    private final ObjectMapper mapper;
    private final Clock clock;

    public JacksonContentManifestValidator(ObjectMapper mapper, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper).copy().findAndRegisterModules();
        this.clock = Objects.requireNonNull(clock);
    }

    @Override public Validation validate(String manifestJson) {
        List<ContentImportIssue> issues = new ArrayList<>();
        JsonNode root;
        try { root = mapper.readTree(manifestJson); }
        catch (Exception ignored) { return Validation.invalid(List.of(issue("INVALID_JSON", "/", "manifest must be valid JSON"))); }
        if (!root.isObject()) return Validation.invalid(List.of(issue("JSON_SCHEMA", "/", "manifest must be an object")));
        root.fieldNames().forEachRemaining(field -> { if (!ROOT_FIELDS.contains(field)) issues.add(issue("JSON_SCHEMA", "/" + field, "unknown property")); });
        for (String field : ROOT_FIELDS) if (!root.hasNonNull(field)) issues.add(issue("JSON_SCHEMA", "/" + field, "required property is missing"));
        if (!"2.0.1".equals(text(root, "schemaVersion"))) issues.add(issue("JSON_SCHEMA", "/schemaVersion", "schemaVersion must be 2.0.1"));
        JsonNode lesson = root.path("lessonPackage"); JsonNode variants = root.path("skillUnitVariants");
        JsonNode mappings = root.path("episodeMappings"); JsonNode assets = root.path("assets");
        if (!"Lin Muen".equals(text(lesson, "character"))) issues.add(issue("LIN_MUEN_REQUIRED", "/lessonPackage/character", "character must be Lin Muen"));
        if (!text(root, "resourceId").equals(text(lesson, "resourceId"))) issues.add(issue("RESOURCE_ID_MISMATCH", "/lessonPackage/resourceId", "resourceId must match manifest"));
        if (!text(root, "resourceVersion").equals(text(lesson, "resourceVersion"))) issues.add(issue("RESOURCE_VERSION_MISMATCH", "/lessonPackage/resourceVersion", "resourceVersion must match manifest"));
        Set<String> variantIds = ids(variants, "skillUnitVariantId", "DUPLICATE_SKILL_UNIT_VARIANT_ID", issues);
        Set<String> mappingIds = ids(mappings, "episodeMappingId", "DUPLICATE_EPISODE_MAPPING_ID", issues);
        Set<String> assetIds = ids(assets, "assetId", "DUPLICATE_ASSET_ID", issues);
        for (JsonNode variant : array(variants)) {
            if (!variant.path("evidenceCriteria").isArray() || variant.path("evidenceCriteria").isEmpty()) issues.add(issue("EVIDENCE_CRITERIA_REQUIRED", "/skillUnitVariants", "evidence criteria are required"));
            Set<String> criteria = ids(variant.path("evidenceCriteria"), "criterionId", "DUPLICATE_EVIDENCE_CRITERION_ID", issues);
            for (JsonNode required : array(variant.path("completionPolicy").path("requiredCriterionIds"))) if (!criteria.contains(required.asText())) issues.add(issue("COMPLETION_CRITERIA_REFERENCE", "/skillUnitVariants/completionPolicy", "unknown criterion reference"));
        }
        for (JsonNode id : array(lesson.path("skillUnitVariantIds"))) if (!variantIds.contains(id.asText())) issues.add(issue("SKILL_UNIT_REFERENCE", "/lessonPackage/skillUnitVariantIds", "unknown Skill Unit Variant"));
        for (JsonNode id : array(lesson.path("episodeMappingIds"))) if (!mappingIds.contains(id.asText())) issues.add(issue("EPISODE_MAPPING_REFERENCE", "/lessonPackage/episodeMappingIds", "unknown Episode Mapping"));
        int heroes = 0;
        for (JsonNode asset : array(assets)) {
            if (!assetIds.contains(text(asset, "assetId"))) continue;
            if ("IMAGE".equals(text(asset, "mediaType")) && "task_hero".equals(text(asset, "purpose"))) {
                heroes++;
                if (!text(asset, "sceneId").equals(text(lesson, "sceneId"))) issues.add(issue("TASK_HERO_SCENE", "/assets", "task hero scene must match lesson"));
                if (!text(asset, "generationPrompt").contains("Lin Muen")) issues.add(issue("TASK_HERO_PROMPT_CHARACTER", "/assets", "task hero prompt must name Lin Muen"));
            }
            if ("AUDIO".equals(text(asset, "mediaType")) && (!text(asset, "transcriptRef").equals(text(lesson.path("transcript"), "transcriptId"))
                    || !asset.path("audioScript").equals(lesson.path("transcript").path("sentences")))) issues.add(issue("AUDIO_TRANSCRIPT_MISMATCH", "/assets", "audio script must match transcript"));
        }
        if (heroes != 1) issues.add(issue("TASK_HERO_COUNT", "/assets", "exactly one task_hero is required"));
        for (JsonNode mapping : array(mappings)) if (!variantIds.contains(text(mapping, "skillUnitVariantId"))) issues.add(issue("MAPPING_SKILL_REFERENCE", "/episodeMappings", "mapping references unknown Skill Unit Variant"));
        if (!lesson.path("dialogue").equals(lesson.path("transcript").path("sentences"))) issues.add(issue("DIALOGUE_TRANSCRIPT_MISMATCH", "/lessonPackage", "dialogue must match transcript"));
        if (!issues.isEmpty()) return Validation.invalid(issues);
        try { return Validation.valid(toEntry(root)); }
        catch (RuntimeException exception) { return Validation.invalid(List.of(issue("BUSINESS_VALIDATION", "/", "manifest cannot form a draft catalog entry"))); }
    }

    private ResourceCatalogEntry toEntry(JsonNode root) {
        JsonNode lesson = root.path("lessonPackage"); String providerCode = text(root, "provider").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
        ContentProvider provider = new ContentProvider(providerCode, text(root, "provider"), ContentProviderType.INTERNAL);
        ResourceCollection collection = new ResourceCollection(text(root, "collectionId"), providerCode, text(root, "collectionId"),
                AccessScope.valueOf(text(root, "accessScope")), CollectionStatus.ACTIVE, null, "INTERNAL", null, "LEARNER", null);
        JsonNode firstVariant = array(root.path("skillUnitVariants")).getFirst();
        LearningResource resource = new LearningResource(text(root, "resourceId"), providerCode, collection.collectionKey(), ResourceType.SCENARIO_LESSON,
                text(root, "title"), text(lesson.path("story"), "context"), "en", CefrLevel.valueOf(text(root, "level")), text(root, "topic"),
                text(lesson, "sceneId"), text(firstVariant, "communicationGoal"), AccessScope.valueOf(text(root, "accessScope")), PublishStatus.DRAFT,
                null, lesson.path("learnerFit").path("estimatedMinutes").asInt(1));
        List<ResourceAsset> assets = array(root.path("assets")).stream().map(asset -> asset(asset, AccessScope.valueOf(text(root, "accessScope")))).toList();
        List<AssetReference> references = assets.stream().map(asset -> new AssetReference(asset.assetKey(), 0)).toList();
        Set<String> variants = new LinkedHashSet<>(); for (JsonNode value : array(root.path("skillUnitVariants"))) variants.add(text(value, "skillUnitVariantId"));
        ResourceVersion version = new ResourceVersion(resource.resourceKey(), text(root, "resourceVersion"), sha256(root.toString()), root.toString(),
                lesson.path("learnerFit").toString(), "{\"source\":\"content-import\"}", variants, references,
                ResourceVersionStatus.DRAFT, clock.instant(), null);
        return new ResourceCatalogEntry(provider, collection, resource, version, assets);
    }

    private ResourceAsset asset(JsonNode node, AccessScope scope) {
        AssetGenerationMetadata generation = new AssetGenerationMetadata(text(node.path("generation"), "provider"), text(node.path("generation"), "model"), text(node.path("generation"), "modelVersion"), text(node.path("generation"), "promptVersion"));
        AssetMediaType type = AssetMediaType.valueOf(text(node, "mediaType"));
        AssetMetadata metadata = type == AssetMediaType.IMAGE
                ? new ImageAssetMetadata(text(node, "generationPrompt"), generation, strings(node.path("characterReferenceIds")), text(node, "aspectRatio"), ShotType.valueOf(text(node, "shotType").toUpperCase(Locale.ROOT)), enums(node.path("displaySurfaces"), DisplaySurface.class), new FocalPoint(node.path("focalPoint").path("x").asDouble(), node.path("focalPoint").path("y").asDouble()), text(node, "altText"), text(node, "sceneId"), text(node, "continuityGroupId"))
                : new AudioAssetMetadata(text(node, "generationPrompt"), generation, text(node, "speakerRole"), text(node, "voiceId"), text(node, "accent"), node.path("speechRate").asDouble(), text(node, "transcriptRef"), array(node.path("audioScript")).stream().map(line -> new AudioScriptLine(text(line, "sentenceId"), text(line, "speaker"), text(line, "text"))).toList());
        return new ResourceAsset(text(node, "assetId"), text(node, "version"), type, AssetPurpose.valueOf(text(node, "purpose").toUpperCase(Locale.ROOT)), text(node, "assetKey"), text(node, "contentHash"), text(node, "mimeType"), 1, scope, metadata, AssetStatus.ACTIVE, clock.instant());
    }
    private static ContentImportIssue issue(String code, String location, String message) { return new ContentImportIssue(code, location, message); }
    private static String text(JsonNode node, String field) { return node.path(field).asText(""); }
    private static List<JsonNode> array(JsonNode node) { if (!node.isArray()) return List.of(); List<JsonNode> values = new ArrayList<>(); node.forEach(values::add); return values; }
    private static Set<String> strings(JsonNode node) { Set<String> values = new LinkedHashSet<>(); array(node).forEach(value -> values.add(value.asText())); return values; }
    private static <E extends Enum<E>> Set<E> enums(JsonNode node, Class<E> type) { Set<E> values = EnumSet.noneOf(type); array(node).forEach(value -> values.add(Enum.valueOf(type, value.asText().toUpperCase(Locale.ROOT)))); return values; }
    private static Set<String> ids(JsonNode node, String field, String duplicateCode, List<ContentImportIssue> issues) { Set<String> values = new LinkedHashSet<>(); for (JsonNode value : array(node)) if (!values.add(text(value, field))) issues.add(issue(duplicateCode, "/", "duplicate " + field)); return values; }
    private static String sha256(String value) { try { return "sha256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
