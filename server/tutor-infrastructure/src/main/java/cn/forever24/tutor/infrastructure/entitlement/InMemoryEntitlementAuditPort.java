package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementAuditPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryEntitlementAuditPort implements EntitlementAuditPort {

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public synchronized void append(
            long actorUserId,
            String actionCode,
            String entitlementKey,
            String beforeState,
            String afterState,
            Instant occurredAt
    ) {
        entries.add(new Entry(
                actorUserId, actionCode, entitlementKey, beforeState, afterState, occurredAt));
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    public record Entry(
            long actorUserId,
            String actionCode,
            String entitlementKey,
            String beforeState,
            String afterState,
            Instant occurredAt
    ) {
    }
}
