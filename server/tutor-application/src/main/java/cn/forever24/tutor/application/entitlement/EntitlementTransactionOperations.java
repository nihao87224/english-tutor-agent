package cn.forever24.tutor.application.entitlement;

import java.util.function.Supplier;

public interface EntitlementTransactionOperations {

    <T> T execute(Supplier<T> action);
}
