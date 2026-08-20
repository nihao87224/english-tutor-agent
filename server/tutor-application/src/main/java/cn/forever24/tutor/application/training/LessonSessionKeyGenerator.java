package cn.forever24.tutor.application.training;

@FunctionalInterface
public interface LessonSessionKeyGenerator {

    String nextKey();
}
