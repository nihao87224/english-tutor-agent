package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.application.roleplay.RolePlayTask;
import cn.forever24.tutor.application.training.ComprehensionQuestion;
import cn.forever24.tutor.application.training.GuidedSpeakingTask;
import cn.forever24.tutor.application.training.LessonContent;
import cn.forever24.tutor.application.training.LessonContentReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CatalogLessonContentReader implements LessonContentReader {
    private final ResourceCatalogRepository catalogRepository;
    private final ObjectMapper objectMapper;

    public CatalogLessonContentReader(ResourceCatalogRepository catalogRepository, ObjectMapper objectMapper) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public LessonContent read(String resourceId, String resourceVersion) {
        var entry = catalogRepository.findExactVersion(resourceId, resourceVersion)
                .orElseThrow(() -> new IllegalStateException("locked lesson resource is unavailable"));
        try {
            JsonNode root = objectMapper.readTree(entry.resourceVersion().manifestJson());
            JsonNode lessonPackage = root.path("lessonPackage");
            List<ComprehensionQuestion> questions = new ArrayList<>();
            lessonPackage.path("questions").forEach(node -> questions.add(new ComprehensionQuestion(
                    required(node, "questionId"), required(node, "prompt"), required(node, "answer"))));

            List<GuidedSpeakingTask> guided = new ArrayList<>();
            JsonNode practice = lessonPackage.path("practice");
            if (practice.isArray()) {
                practice.forEach(node -> addGuided(node, guided));
            } else if (practice.isObject()) {
                addGuided(practice, guided);
            }
            JsonNode rolePlay = lessonPackage.path("rolePlay");
            RolePlayTask rolePlayTask = rolePlay.isObject() ? new RolePlayTask(
                    required(rolePlay, "taskId"), required(rolePlay, "goal"),
                    required(rolePlay, "userRole"), required(rolePlay, "aiRole"),
                    strings(rolePlay.path("successCriteria")), required(rolePlay, "openingLine")) : null;
            return new LessonContent(questions, guided, rolePlayTask);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("locked lesson content is invalid", exception);
        }
    }

    private static void addGuided(JsonNode node, List<GuidedSpeakingTask> guided) {
        if (!"guided_speaking".equalsIgnoreCase(node.path("type").asText())) return;
        guided.add(new GuidedSpeakingTask(
                required(node, "taskId"), required(node, "prompt"), strings(node.path("successCriteria")),
                strings(node.path("scaffolding"))));
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) values.add(value.asText());
        });
        return values;
    }

    private static String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
