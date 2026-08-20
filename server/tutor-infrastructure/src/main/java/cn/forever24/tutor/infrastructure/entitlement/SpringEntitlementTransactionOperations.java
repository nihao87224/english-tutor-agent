package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementTransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

public final class SpringEntitlementTransactionOperations implements EntitlementTransactionOperations {

    private final TransactionTemplate transactionTemplate;

    public SpringEntitlementTransactionOperations(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate);
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
