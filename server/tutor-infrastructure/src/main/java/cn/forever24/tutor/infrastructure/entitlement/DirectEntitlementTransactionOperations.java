package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementTransactionOperations;

import java.util.function.Supplier;

public final class DirectEntitlementTransactionOperations implements EntitlementTransactionOperations {

    @Override
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }
}
