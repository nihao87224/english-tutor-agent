package cn.forever24.tutor.application.entitlement;

import java.time.Instant;

public interface EntitlementAuditPort {

    void append(
            long actorUserId,
            String actionCode,
            String entitlementKey,
            String beforeState,
            String afterState,
            Instant occurredAt);
}
