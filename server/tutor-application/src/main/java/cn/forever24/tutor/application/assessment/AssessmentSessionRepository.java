package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.profile.UserKey;

public interface AssessmentSessionRepository {

    AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes);
}
