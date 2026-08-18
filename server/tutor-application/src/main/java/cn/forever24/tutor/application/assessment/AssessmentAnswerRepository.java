package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.UserKey;

import java.util.Set;

public interface AssessmentAnswerRepository {

    AssessmentAnswerReceipt saveObjectiveAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredObjectiveAnswer answer);

    AssessmentAnswerReceipt saveOpenAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredOpenAnswer answer);

    Set<String> answeredItemIds(UserKey userKey, String assessmentId);
}
