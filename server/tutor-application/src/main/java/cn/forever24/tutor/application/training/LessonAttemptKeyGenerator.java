package cn.forever24.tutor.application.training;

@FunctionalInterface
public interface LessonAttemptKeyGenerator {
    String nextKey();
}
