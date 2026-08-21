package cn.forever24.tutor.application.resource;

import java.time.Instant;

public interface CatalogManagementAuditPort {
    void append(long actorUserId, String action, String targetKey, Instant occurredAt);
}
