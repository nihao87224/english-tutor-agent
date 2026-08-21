package cn.forever24.tutor.infrastructure.audio;

import cn.forever24.tutor.application.audio.AudioRetentionApplicationService;
import org.springframework.scheduling.annotation.Scheduled;

public final class AudioRetentionScheduler {
    private final AudioRetentionApplicationService service;

    public AudioRetentionScheduler(AudioRetentionApplicationService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${tutor.audio.retention-initial-delay:PT5M}",
            fixedDelayString = "${tutor.audio.retention-sweep-delay:PT15M}")
    public void sweep() {
        service.deleteExpired(100);
    }
}
