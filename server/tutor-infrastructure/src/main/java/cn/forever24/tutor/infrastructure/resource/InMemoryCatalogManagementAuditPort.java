package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogManagementAuditPort;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCatalogManagementAuditPort implements CatalogManagementAuditPort {
    private final List<String> events = new CopyOnWriteArrayList<>();

    @Override
    public void append(long actorUserId, String action, String targetKey, Instant occurredAt) {
        events.add(actorUserId + ":" + action + ":" + targetKey + ":" + occurredAt);
    }
}
