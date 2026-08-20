package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementRepository;
import cn.forever24.tutor.application.entitlement.ResourceAccessTargetRepository;

public interface EntitlementStoreAdapter extends EntitlementRepository, ResourceAccessTargetRepository {
}
