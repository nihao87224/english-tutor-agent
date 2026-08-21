package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;

public record AnalysisRetryJob(UserKey userKey, String sessionId, String attemptId, int attemptCount) {
    public AnalysisRetryJob {
        if (userKey == null || sessionId == null || sessionId.isBlank() || attemptId == null || attemptId.isBlank()
                || attemptCount < 1) throw new IllegalArgumentException("valid retry job fields are required");
    }
}
