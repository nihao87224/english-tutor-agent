package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonSession;

public interface LessonEvidenceRepository {
    /** Must be idempotent per attempt and run in the caller's transaction. */
    EvidenceSummary record(UserKey userKey, LessonSession session, LessonAttempt attempt);
}
