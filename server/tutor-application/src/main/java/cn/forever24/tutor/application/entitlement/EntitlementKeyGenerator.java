package cn.forever24.tutor.application.entitlement;

@FunctionalInterface
public interface EntitlementKeyGenerator {

    String nextKey();
}
