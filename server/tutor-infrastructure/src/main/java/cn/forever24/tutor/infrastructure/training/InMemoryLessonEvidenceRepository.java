package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.EvidenceSummary;
import cn.forever24.tutor.application.training.LessonEvidenceRepository;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLessonEvidenceRepository implements LessonEvidenceRepository {
    private final Map<String, EvidenceSummary> values = new ConcurrentHashMap<>();

    @Override
    public EvidenceSummary record(UserKey userKey, LessonSession session, LessonAttempt attempt) {
        return values.computeIfAbsent(userKey.value() + "|" + attempt.attemptId(), ignored -> {
            var skills = java.util.List.of("speaking");
            long failed = attempt.analysis().criteria().stream().filter(value -> !value.satisfied()).count();
            String nextFocus = failed == 0 ? "Use this communication pattern in a new situation."
                    : "Revisit the feedback before trying this communication goal again.";
            return new EvidenceSummary(attempt.attemptId(), 1, skills, nextFocus);
        });
    }
}
