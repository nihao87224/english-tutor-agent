package cn.forever24.tutor.api.roleplay;

import cn.forever24.tutor.training.RolePlayTurn;

import java.time.Instant;

public record RolePlayTurnResponse(
        String turnId,
        String attemptId,
        String taskId,
        String learnerText,
        String replyText,
        String status,
        String errorCode,
        Instant acceptedAt,
        Instant completedAt,
        long version
) {
    static RolePlayTurnResponse from(RolePlayTurn turn) {
        return new RolePlayTurnResponse(
                turn.turnId(), turn.attemptId(), turn.taskId(), turn.learnerText(), turn.replyText(),
                turn.status().name(), turn.errorCode(), turn.acceptedAt(), turn.completedAt(), turn.version());
    }
}
