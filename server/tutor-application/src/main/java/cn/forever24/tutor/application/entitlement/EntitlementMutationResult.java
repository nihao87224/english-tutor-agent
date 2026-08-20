package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.entitlement.Entitlement;

import java.util.Objects;

public record EntitlementMutationResult(Entitlement entitlement, EntitlementMutationOutcome outcome) {
    public EntitlementMutationResult {
        Objects.requireNonNull(entitlement, "entitlement must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
    }
}
