package cn.forever24.tutor.ai.conversation;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.conversation.ConversationStreamContext;
import cn.forever24.tutor.application.conversation.ConversationStreamEvent;
import cn.forever24.tutor.application.conversation.CorrectionAnalysisContext;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProviderConversationReplyStreamer implements ConversationReplyStreamer {

    private static final String PROMPT_VERSION = "conversation-coach-v1";
    private static final String SCHEMA_VERSION = "conversation-stream-v1";

    private final ChatProvider chatProvider;
    private final CorrectionAnalyzer correctionAnalyzer;

    public ProviderConversationReplyStreamer(ChatProvider chatProvider, CorrectionAnalyzer correctionAnalyzer) {
        this.chatProvider = chatProvider;
        this.correctionAnalyzer = correctionAnalyzer;
    }

    @Override
    public List<ConversationStreamEvent> streamReply(ConversationStreamContext context) {
        ChatStream stream = chatProvider.stream(new ChatProviderRequest(
                "conversation-" + UUID.randomUUID(),
                PROMPT_VERSION,
                SCHEMA_VERSION,
                promptInput(context),
                Map.of(
                        "sessionId", context.session().sessionId(),
                        "taskId", context.currentTask().taskId(),
                        "taskType", context.currentTask().type())));

        List<ConversationStreamEvent> events = new ArrayList<>();
        long eventId = 1;
        events.add(ConversationStreamEvent.status(eventId++, "THINKING", "Understanding your message..."));
        for (String chunk : stream.chunks()) {
            events.add(ConversationStreamEvent.textDelta(eventId++, chunk));
        }
        events.add(ConversationStreamEvent.correctionReady(eventId++, correctionAnalyzer.analyze(
                new CorrectionAnalysisContext(context.session(), context.currentTask(), context.message()))));
        events.add(ConversationStreamEvent.done(
                eventId,
                stream.trace().traceId(),
                stream.trace().providerId(),
                stream.trace().modelId()));
        return events;
    }

    private String promptInput(ConversationStreamContext context) {
        return """
                You are an English tutor. Keep the reply natural and concise.
                Current task: %s
                Task reason: %s
                Learner message: %s
                """.formatted(
                context.currentTask().title(),
                context.currentTask().reason(),
                context.message());
    }
}
