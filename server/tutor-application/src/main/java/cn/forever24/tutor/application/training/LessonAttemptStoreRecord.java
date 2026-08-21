package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonAttempt;

public record LessonAttemptStoreRecord(String requestHash, LessonAttempt attempt) {
}
