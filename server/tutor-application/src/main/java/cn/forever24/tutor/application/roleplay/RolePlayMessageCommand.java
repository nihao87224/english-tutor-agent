package cn.forever24.tutor.application.roleplay;

public record RolePlayMessageCommand(
        String taskId,
        String text,
        String audioAssetId,
        String conversationTurnId
) {
    public RolePlayMessageCommand {
        taskId = required(taskId, "taskId");
        conversationTurnId = required(conversationTurnId, "conversationTurnId");
        if (taskId.length() > 128) throw new IllegalArgumentException("taskId must not exceed 128 characters");
        if (conversationTurnId.length() > 128) {
            throw new IllegalArgumentException("conversationTurnId must not exceed 128 characters");
        }
        text = normalize(text);
        audioAssetId = normalize(audioAssetId);
        if ((text == null) == (audioAssetId == null)) {
            throw new IllegalArgumentException("exactly one of text or audioAssetId is required");
        }
        if (text != null && text.length() > 4000) throw new IllegalArgumentException("text must not exceed 4000 characters");
        if (audioAssetId != null && audioAssetId.length() > 96) {
            throw new IllegalArgumentException("audioAssetId must not exceed 96 characters");
        }
    }

    private static String required(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
