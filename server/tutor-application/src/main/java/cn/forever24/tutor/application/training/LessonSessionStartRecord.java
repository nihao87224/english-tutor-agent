package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonSession;

public record LessonSessionStartRecord(String requestHash, LessonSession session) {
}
