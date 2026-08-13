package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.fake.FakeChatProvider;
import cn.forever24.tutor.application.conversation.ConversationStreamContext;
import cn.forever24.tutor.application.conversation.ConversationStreamEvent;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderConversationReplyStreamerTest {

    @Test
    void convertsProviderChunksToOrderedSseEvents() {
        FakeChatProvider chatProvider = new FakeChatProvider();
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
}
