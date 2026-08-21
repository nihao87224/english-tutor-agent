package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.AnalysisRetryApplicationService;
import org.springframework.scheduling.annotation.Scheduled;

public final class AnalysisRetryScheduler {
    private final AnalysisRetryApplicationService service;
    public AnalysisRetryScheduler(AnalysisRetryApplicationService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${tutor.analysis.retry-delay-ms:5000}")
    public void retryDueJobs() { service.retryDueJobs(); }
}
