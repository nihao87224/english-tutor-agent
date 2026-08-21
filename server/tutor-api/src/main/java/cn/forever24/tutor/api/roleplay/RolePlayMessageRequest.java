package cn.forever24.tutor.api.roleplay;

import cn.forever24.tutor.application.roleplay.RolePlayMessageCommand;

public record RolePlayMessageRequest(
        String taskId,
        String text,
        String audioAssetId,
        String conversationTurnId
) {
    RolePlayMessageCommand toCommand() {
        return new RolePlayMessageCommand(taskId, text, audioAssetId, conversationTurnId);
    }
}
