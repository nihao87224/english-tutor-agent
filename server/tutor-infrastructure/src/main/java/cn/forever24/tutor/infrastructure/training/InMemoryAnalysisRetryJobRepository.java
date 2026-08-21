package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.AnalysisRetryJob;
import cn.forever24.tutor.application.training.AnalysisRetryJobRepository;
import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAnalysisRetryJobRepository implements AnalysisRetryJobRepository {
    private final Map<String, Stored> jobs = new ConcurrentHashMap<>();
    public void schedule(UserKey userKey, String sessionId, String attemptId, String errorCode, Instant nextRunAt) {
        jobs.compute(attemptId, (key, old) -> old == null ? new Stored(new AnalysisRetryJob(userKey, sessionId, attemptId, 1), nextRunAt, "PENDING") : old);
    }
    public synchronized List<AnalysisRetryJob> claimDue(Instant now, int limit, String workerId) {
        return jobs.values().stream().filter(value -> value.status.equals("PENDING") && !value.nextRunAt.isAfter(now)).limit(limit)
                .peek(value -> value.status = "RUNNING").map(value -> value.job).toList();
    }
    public void complete(AnalysisRetryJob job) { jobs.remove(job.attemptId()); }
    public void reschedule(AnalysisRetryJob job, String errorCode, Instant nextRunAt) {
        jobs.computeIfPresent(job.attemptId(), (key, old) -> new Stored(new AnalysisRetryJob(job.userKey(), job.sessionId(), job.attemptId(), job.attemptCount() + 1), nextRunAt, "PENDING"));
    }
    public void failFinal(AnalysisRetryJob job, String errorCode) { jobs.remove(job.attemptId()); }
    private static final class Stored { private final AnalysisRetryJob job; private final Instant nextRunAt; private String status;
        private Stored(AnalysisRetryJob job, Instant nextRunAt, String status) { this.job = job; this.nextRunAt = nextRunAt; this.status = status; } }
}
