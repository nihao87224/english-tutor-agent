package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluationRequest;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.OpenAnswerEvaluation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

public class OpenAiOpenAnswerEvaluator implements OpenAnswerEvaluator {

    static final String PROMPT_VERSION = "open-answer-evaluator-v1";
    static final String SCHEMA_VERSION = "open-answer-evaluation-v1";

    private static final JsonSchema EVALUATION_SCHEMA = new JsonSchema(
            "open-answer-evaluation",
            SCHEMA_VERSION,
            Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "required", java.util.List.of("correctness", "score", "confidence", "feedback"),
                    "properties", Map.of(
                            "correctness", Map.of("type", "string", "enum", java.util.List.of("CORRECT", "PARTIAL", "INCORRECT", "UNSCORED")),
                            "score", Map.of("type", "number", "minimum", 0, "maximum", 1),
                            "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                            "feedback", Map.of("type", "string", "minLength", 1, "maxLength", 500))));

    private final ChatProvider chatProvider;
    private final ObjectMapper objectMapper;

    public OpenAiOpenAnswerEvaluator(ChatProvider chatProvider, ObjectMapper objectMapper) {
        this.chatProvider = chatProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    @Override
    public String schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public OpenAnswerEvaluation evaluate(OpenAnswerEvaluationRequest request) {
        StructuredResponse response = chatProvider.completeStructured(
                ChatProviderRequest.structured(
                        "open-answer-" + java.util.UUID.randomUUID(),
                        PROMPT_VERSION,
                        SCHEMA_VERSION,
                        prompt(request)),
                EVALUATION_SCHEMA);
        try {
            JsonNode root = objectMapper.readTree(response.content());
            return new OpenAnswerEvaluation(
                    AssessmentCorrectness.valueOf(root.path("correctness").asText()),
                    decimal(root.path("score")),
                    decimal(root.path("confidence")),
                    root.path("feedback").asText(),
                    PROMPT_VERSION,
                    SCHEMA_VERSION);
        } catch (Exception exception) {
            return OpenAnswerEvaluation.safeUnscored(PROMPT_VERSION, SCHEMA_VERSION);
        }
    }

    private String prompt(OpenAnswerEvaluationRequest request) {
        return """
                Evaluate this English learner answer for practice feedback.
                Return JSON only with correctness, score, confidence, and feedback.
                Item id: %s
                Item type: %s
                Learner answer: %s
                """.formatted(request.item().itemId(), request.item().questionType(), request.text());
    }

    private BigDecimal decimal(JsonNode node) {
        return new BigDecimal(node.asText("0"));
    }
}
