package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonAttemptStatus;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class AnalysisRetryApplicationService {
    private static final int MAX_ANALYSIS_ATTEMPTS = 3;
    private final AnalysisRetryJobRepository jobs;
    private final LessonAttemptApplicationService attempts;
    private final Clock clock;

    public AnalysisRetryApplicationService(AnalysisRetryJobRepository jobs, LessonAttemptApplicationService attempts, Clock clock) {
        this.jobs = Objects.requireNonNull(jobs);
        this.attempts = Objects.requireNonNull(attempts);
        this.clock = Objects.requireNonNull(clock);
    }

    public int retryDueJobs() {
        var claimed = jobs.claimDue(clock.instant(), 20, "analysis-worker-" + UUID.randomUUID());
        for (AnalysisRetryJob job : claimed) {
            var result = attempts.analyzePending(job.userKey().value(), job.sessionId(), job.attemptId());
            if (result.attempt().status() == LessonAttemptStatus.ANALYSIS_RETRYABLE && job.attemptCount() < MAX_ANALYSIS_ATTEMPTS) {
                jobs.reschedule(job, result.attempt().analysisErrorCode(), clock.instant().plus(backoff(job.attemptCount())));
            } else if (result.attempt().status() == LessonAttemptStatus.ANALYSIS_RETRYABLE) {
                attempts.markAnalysisFinalFailure(job.userKey().value(), job.sessionId(), job.attemptId());
                jobs.failFinal(job, "ANALYSIS_RETRY_LIMIT_REACHED");
            } else {
                jobs.complete(job);
            }
        }
        return claimed.size();
    }

    private static Duration backoff(int attemptCount) {
        return Duration.ofSeconds(Math.min(60, 5L << Math.min(3, attemptCount - 1)));
    }
}
