package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.AssessmentSessionRepository;
import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.assessment.AssessmentSessionStatus;
import cn.forever24.tutor.profile.UserKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAssessmentSessionRepository implements AssessmentSessionRepository {

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<UserKey, AssessmentSession> activeInitialSessions = new ConcurrentHashMap<>();

    @Override
    public AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes) {
        return activeInitialSessions.computeIfAbsent(userKey, ignored -> new AssessmentSession(
                "assessment-" + sequence.incrementAndGet(),
                AssessmentSessionStatus.IN_PROGRESS,
                targetMinutes,
                targetMinutes));
    }
}
