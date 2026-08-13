package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.FourSkillSelfAssessment;
import cn.forever24.tutor.assessment.SelfAssessmentResult;
import cn.forever24.tutor.profile.UserKey;

public interface SelfAssessmentRepository {

    SelfAssessmentResult save(UserKey userKey, FourSkillSelfAssessment assessment);
}
