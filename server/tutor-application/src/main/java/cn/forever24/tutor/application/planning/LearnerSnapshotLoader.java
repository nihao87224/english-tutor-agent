package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.profile.UserKey;

public interface LearnerSnapshotLoader {

    LearnerPlanningSnapshot load(UserKey userKey);
}
