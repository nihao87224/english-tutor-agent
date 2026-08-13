package cn.forever24.tutor.api.conversation;

public record ConversationMessageRequest(
        String messageType,
        String text,
        String taskId
) {
}
