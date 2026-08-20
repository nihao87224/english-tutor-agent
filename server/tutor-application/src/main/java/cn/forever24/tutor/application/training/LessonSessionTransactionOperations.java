package cn.forever24.tutor.application.training;

import java.util.function.Supplier;

public interface LessonSessionTransactionOperations {

    <T> T execute(Supplier<T> action);
}
