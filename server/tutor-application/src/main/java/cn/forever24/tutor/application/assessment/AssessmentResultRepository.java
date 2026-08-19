package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.profile.UserKey;

public interface AssessmentResultRepository {

    AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId);

    AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId);

    boolean hasCompletedInitialAssessmentResult(UserKey userKey);
}
