package cn.forever24.tutor.application.conversation;

public record ConversationStreamRequest(
        String userKey,
        String sessionId,
        ConversationMessageType messageType,
        String text,
        String taskId,
        String idempotencyKey
) {

    private static final int MAX_TEXT_LENGTH = 4000;

    public ConversationStreamRequest {
        if (userKey == null || userKey.isBlank()) {
            throw new IllegalArgumentException("userKey is required");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("messageType is required");
        }
        if (messageType != ConversationMessageType.TEXT) {
            throw new IllegalArgumentException("only TEXT conversation messages are supported in M2-T03");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must be at most 4000 characters");
        }
        if (taskId != null && taskId.isBlank()) {
            taskId = null;
        }
    }
}
