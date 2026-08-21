package cn.forever24.tutor.application.training;

public interface LessonContentReader {
    LessonContent read(String resourceId, String resourceVersion);
}
