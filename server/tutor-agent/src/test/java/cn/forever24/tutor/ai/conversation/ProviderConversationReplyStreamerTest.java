package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.conversation.ConversationStreamContext;
import cn.forever24.tutor.application.conversation.ConversationStreamEvent;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderConversationReplyStreamerTest {

    @Test
    void convertsProviderChunksToOrderedSseEvents() {
        StubChatProvider chatProvider = new StubChatProvider();
        ProviderConversationReplyStreamer streamer = new ProviderConversationReplyStreamer(
                chatProvider,
                new ProviderLayeredCorrectionAnalyzer(chatProvider));

        List<ConversationStreamEvent> events = streamer.streamReply(new ConversationStreamContext(
                TrainingSession.startDaily(
                        "training-1",
                        "plan-1",
                        TrainingSessionMode.TEXT,
                        "task-1",
                        Instant.parse("2026-08-10T08:00:00Z")),
                new LearningPlanTask(
                        "task-1",
                        "CONVERSATION",
                        "Ask a follow-up question",
                        10,
                        List.of("speaking"),
                        "A2",
                        "Practice natural follow-up questions."),
                "I very like this movie because it is exciting."));

        assertEquals("status", events.get(0).type().eventName());
        assertEquals("text_delta", events.get(1).type().eventName());
        assertEquals("correction_ready", events.get(events.size() - 2).type().eventName());
        assertEquals("done", events.get(events.size() - 1).type().eventName());
        assertEquals("This is ", events.get(1).data().get("delta"));
        assertEquals(true, events.get(events.size() - 2).data().get("hasError"));
    }

    private static final class StubChatProvider implements ChatProvider {
        @Override
        public ChatStream stream(ChatProviderRequest request) {
            return new ChatStream(
                    List.of("This is ", "a real-provider-shaped response."),
                    trace(request),
                    ProviderUsage.freeText(10, 8));
        }

        @Override
        public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
            return new StructuredResponse("{}", trace(request), ProviderUsage.freeText(10, 1), false);
        }

        @Override
        public ProviderCapabilities capabilities() {
            return new ProviderCapabilities("test-openai", "test-model", Set.of(ProviderCapability.CHAT_STREAMING), java.time.Duration.ofSeconds(1));
        }

        private ProviderTrace trace(ChatProviderRequest request) {
            return new ProviderTrace(request.traceId(), "test-openai", "test-model", request.promptVersion(), request.schemaVersion());
        }
    }
}
