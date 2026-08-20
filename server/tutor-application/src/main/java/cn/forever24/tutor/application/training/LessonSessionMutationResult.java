package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonSession;

public record LessonSessionMutationResult(LessonSession session, boolean replayed) {
}
