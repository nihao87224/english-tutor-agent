package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonSessionTransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

public final class SpringLessonSessionTransactionOperations implements LessonSessionTransactionOperations {

    private final TransactionTemplate transactionTemplate;

    public SpringLessonSessionTransactionOperations(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate);
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
