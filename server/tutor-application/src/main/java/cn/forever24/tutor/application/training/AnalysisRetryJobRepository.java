package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.util.List;

public interface AnalysisRetryJobRepository {
    void schedule(UserKey userKey, String sessionId, String attemptId, String errorCode, Instant nextRunAt);
    List<AnalysisRetryJob> claimDue(Instant now, int limit, String workerId);
    void complete(AnalysisRetryJob job);
    void reschedule(AnalysisRetryJob job, String errorCode, Instant nextRunAt);
    void failFinal(AnalysisRetryJob job, String errorCode);
}
