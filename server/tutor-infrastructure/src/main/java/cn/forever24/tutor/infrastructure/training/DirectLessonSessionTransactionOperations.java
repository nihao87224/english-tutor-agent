package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonSessionTransactionOperations;

import java.util.function.Supplier;

public final class DirectLessonSessionTransactionOperations implements LessonSessionTransactionOperations {

    @Override
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }
}
