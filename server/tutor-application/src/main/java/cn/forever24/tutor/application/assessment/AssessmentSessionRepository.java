package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.profile.UserKey;

import java.util.Optional;

public interface AssessmentSessionRepository {

    AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes);

    Optional<AssessmentSession> findActiveInitialAssessment(UserKey userKey);
}
