package cn.forever24.tutor.training;

import java.time.Instant;

public record RolePlayTurn(
        String turnId,
        String sessionId,
        String attemptId,
        String taskId,
        String learnerText,
        String replyText,
        RolePlayTurnStatus status,
        String promptVersion,
        String providerId,
        String modelId,
        String traceId,
        String errorCode,
        Instant acceptedAt,
        Instant completedAt,
        long version
) {
    public RolePlayTurn {
        require(turnId, "turnId");
        require(sessionId, "sessionId");
        require(attemptId, "attemptId");
        require(taskId, "taskId");
        if (status == null || acceptedAt == null || version < 1) {
            throw new IllegalArgumentException("role-play status, acceptedAt and positive version are required");
        }
        if (status == RolePlayTurnStatus.COMPLETED) {
            require(learnerText, "learnerText");
            require(replyText, "replyText");
            require(promptVersion, "promptVersion");
            require(providerId, "providerId");
            require(modelId, "modelId");
            require(traceId, "traceId");
            if (completedAt == null) throw new IllegalArgumentException("completedAt is required");
        }
    }

    public static RolePlayTurn accepted(
            String turnId, String sessionId, String attemptId, String taskId,
            String learnerText, boolean awaitingTranscript, Instant now
    ) {
        return new RolePlayTurn(turnId, sessionId, attemptId, taskId, learnerText, null,
                awaitingTranscript ? RolePlayTurnStatus.AWAITING_TRANSCRIPT : RolePlayTurnStatus.ACCEPTED,
                null, null, null, null, null, now, null, 1);
    }

    public RolePlayTurn withLearnerText(String text) {
        require(text, "learnerText");
        return new RolePlayTurn(turnId, sessionId, attemptId, taskId, text, null,
                RolePlayTurnStatus.ACCEPTED, null, null, null, null, null,
                acceptedAt, null, version + 1);
    }

    public RolePlayTurn complete(
            String reply, String nextPromptVersion, String nextProviderId,
            String nextModelId, String nextTraceId, Instant now
    ) {
        return new RolePlayTurn(turnId, sessionId, attemptId, taskId, learnerText, reply,
                RolePlayTurnStatus.COMPLETED, nextPromptVersion, nextProviderId, nextModelId,
                nextTraceId, null, acceptedAt, now, version + 1);
    }

    public RolePlayTurn fail(String code, boolean retryable) {
        require(code, "errorCode");
        return new RolePlayTurn(turnId, sessionId, attemptId, taskId, learnerText, replyText,
                retryable ? RolePlayTurnStatus.FAILED_RETRYABLE : RolePlayTurnStatus.FAILED_FINAL,
                promptVersion, providerId, modelId, traceId, code, acceptedAt, completedAt, version + 1);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
