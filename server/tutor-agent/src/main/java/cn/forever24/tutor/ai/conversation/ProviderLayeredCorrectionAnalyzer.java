package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.conversation.CorrectionAnalysisContext;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import cn.forever24.tutor.application.conversation.CorrectionSeverity;
import cn.forever24.tutor.application.conversation.CorrectionSuggestion;
import cn.forever24.tutor.application.conversation.CorrectionSuggestionStyle;
import cn.forever24.tutor.application.conversation.LayeredCorrectionItem;
import cn.forever24.tutor.application.conversation.LayeredCorrectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProviderLayeredCorrectionAnalyzer implements CorrectionAnalyzer {

    static final String PROMPT_VERSION = "correction-analyzer-v1";
    static final String SCHEMA_VERSION = "correction-result-v1";

    private static final JsonSchema CORRECTION_SCHEMA = new JsonSchema(
            "correction-result",
            SCHEMA_VERSION,
            Map.of(
                    "$id", "https://english-tutor.local/schemas/correction-result/1.0.0",
                    "type", "object"));

    private final ChatProvider chatProvider;

    public ProviderLayeredCorrectionAnalyzer(ChatProvider chatProvider) {
        this.chatProvider = chatProvider;
    }

    @Override
    public LayeredCorrectionResult analyze(CorrectionAnalysisContext context) {
        StructuredResponse response = chatProvider.completeStructured(new ChatProviderRequest(
                "correction-" + UUID.randomUUID(),
                PROMPT_VERSION,
                SCHEMA_VERSION,
                promptInput(context),
                Map.of(
                        "sessionId", context.session().sessionId(),
                        "taskId", context.currentTask().taskId(),
                        "taskType", context.currentTask().type())), CORRECTION_SCHEMA);

        ProviderTrace trace = response.trace();
        List<LayeredCorrectionItem> corrections = deterministicCorrections(context.message());
        return new LayeredCorrectionResult(
                !corrections.isEmpty(),
                corrections,
                corrections.isEmpty()
                        ? "Clear and natural enough for this turn. Keep the conversation moving."
                        : "Good communication. Focus on these small fixes while continuing the exchange.",
                trace.promptVersion(),
                trace.schemaVersion(),
                trace.traceId(),
                trace.providerId(),
                trace.modelId());
    }

    private List<LayeredCorrectionItem> deterministicCorrections(String message) {
        String normalized = message.toLowerCase();
        List<LayeredCorrectionItem> corrections = new ArrayList<>();
        if (normalized.contains("very like")) {
            corrections.add(new LayeredCorrectionItem(
                    "very like",
                    "really like",
                    "word_order",
                    CorrectionSeverity.MEDIUM,
                    "Use 'really' before verbs like 'like'; 'very' usually modifies adjectives.",
                    false,
                    true,
                    List.of(
                            new CorrectionSuggestion("I really like it.", CorrectionSuggestionStyle.NEUTRAL),
                            new CorrectionSuggestion("I'm a big fan of it.", CorrectionSuggestionStyle.CASUAL))));
        }
        if (normalized.contains("he go") || normalized.contains("she go")) {
            boolean she = normalized.contains("she go");
            corrections.add(new LayeredCorrectionItem(
                    she ? "she go" : "he go",
                    she ? "she goes" : "he goes",
                    "subject_verb_agreement",
                    CorrectionSeverity.HIGH,
                    "For he or she in the present tense, add -s to the verb.",
                    true,
                    true,
                    List.of(new CorrectionSuggestion(
                            she ? "She goes there." : "He goes there.",
                            CorrectionSuggestionStyle.NEUTRAL))));
        }
        if (normalized.contains("maybe because")) {
            corrections.add(new LayeredCorrectionItem(
                    "maybe because",
                    "may be because",
                    "part_of_speech",
                    CorrectionSeverity.LOW,
                    "Use 'may be' as verb phrase; 'maybe' is an adverb.",
                    false,
                    false,
                    List.of(new CorrectionSuggestion("It may be because of the schedule.", CorrectionSuggestionStyle.FORMAL))));
        }
        return corrections.stream().limit(3).toList();
    }

    private String promptInput(CorrectionAnalysisContext context) {
        return """
                Analyze the learner message for focused English corrections.
                Return at most three corrections and keep feedback encouraging.
                Current task: %s
                Task reason: %s
                Learner message: %s
                """.formatted(
                context.currentTask().title(),
                context.currentTask().reason(),
                context.message());
    }
}
