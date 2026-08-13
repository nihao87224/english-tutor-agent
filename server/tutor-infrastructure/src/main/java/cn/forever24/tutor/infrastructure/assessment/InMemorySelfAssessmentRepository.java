package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.SelfAssessmentRepository;
import cn.forever24.tutor.assessment.FourSkillSelfAssessment;
import cn.forever24.tutor.assessment.SelfAssessmentResult;
import cn.forever24.tutor.profile.UserKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemorySelfAssessmentRepository implements SelfAssessmentRepository {

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<String, FourSkillSelfAssessment> assessments = new ConcurrentHashMap<>();

    @Override
    public SelfAssessmentResult save(UserKey userKey, FourSkillSelfAssessment assessment) {
        String assessmentId = "self-" + sequence.incrementAndGet();
        assessments.put(assessmentId, assessment);
        return new SelfAssessmentResult(assessmentId, assessment.estimatedBand());
    }
}
