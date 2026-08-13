package cn.forever24.tutor.application.conversation;

import java.util.Map;

public record ConversationStreamEvent(
        long id,
        ConversationStreamEventType type,
        Map<String, Object> data
) {

    public ConversationStreamEvent {
        if (id <= 0) {
            throw new IllegalArgumentException("event id must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("event type is required");
        }
        data = Map.copyOf(data == null ? Map.of() : data);
    }

    public static ConversationStreamEvent status(long id, String stage, String message) {
        return new ConversationStreamEvent(id, ConversationStreamEventType.STATUS, Map.of(
                "stage", stage,
                "message", message));
    }

    public static ConversationStreamEvent textDelta(long id, String delta) {
        if (delta == null || delta.isBlank()) {
            throw new IllegalArgumentException("delta is required");
        }
        return new ConversationStreamEvent(id, ConversationStreamEventType.TEXT_DELTA, Map.of("delta", delta));
    }

    public static ConversationStreamEvent correctionReady(long id, LayeredCorrectionResult correction) {
        if (correction == null) {
            throw new IllegalArgumentException("correction is required");
        }
        return new ConversationStreamEvent(id, ConversationStreamEventType.CORRECTION_READY, correction.toEventData());
    }

    public static ConversationStreamEvent done(long id, String traceId, String providerId, String modelId) {
        return new ConversationStreamEvent(id, ConversationStreamEventType.DONE, Map.of(
                "traceId", traceId,
                "providerId", providerId,
                "modelId", modelId));
    }
}
