package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementApplicationException;
import cn.forever24.tutor.application.entitlement.ResourceAccessTarget;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryEntitlementRepository implements EntitlementStoreAdapter {

    private final Map<String, Entitlement> entitlements = new LinkedHashMap<>();
    private final Map<String, ResourceAccessTarget> targets = new LinkedHashMap<>();

    @Override
    public synchronized Optional<Entitlement> find(UserKey userKey, String collectionKey) {
        return Optional.ofNullable(entitlements.get(key(userKey, collectionKey)));
    }

    @Override
    public synchronized Optional<Entitlement> findForUpdate(UserKey userKey, String collectionKey) {
        return find(userKey, collectionKey);
    }

    @Override
    public synchronized List<Entitlement> findForUser(UserKey userKey) {
        return entitlements.values().stream()
                .filter(entitlement -> entitlement.userKey().equals(userKey))
                .toList();
    }

    @Override
    public synchronized void insert(Entitlement entitlement) {
        String key = key(entitlement.userKey(), entitlement.collectionKey());
        if (entitlements.putIfAbsent(key, entitlement) != null) {
            throw EntitlementApplicationException.conflict(
                    "ENTITLEMENT_ALREADY_EXISTS", "entitlement already exists");
        }
    }

    @Override
    public synchronized void update(Entitlement entitlement, long expectedVersion) {
        String key = key(entitlement.userKey(), entitlement.collectionKey());
        Entitlement current = entitlements.get(key);
        if (current == null || current.version() != expectedVersion) {
            throw EntitlementApplicationException.conflict(
                    "ENTITLEMENT_VERSION_CONFLICT", "entitlement version changed");
        }
        entitlements.put(key, entitlement);
    }

    @Override
    public synchronized Optional<ResourceAccessTarget> findByResourceKey(String resourceKey) {
        return Optional.ofNullable(targets.get(resourceKey));
    }

    @Override
    public synchronized boolean collectionExists(String collectionKey) {
        return targets.values().stream()
                .anyMatch(target -> target.collection().collectionKey().equals(collectionKey));
    }

    public synchronized void register(ResourceAccessTarget target) {
        targets.put(target.resource().resourceKey(), target);
    }

    public synchronized List<Entitlement> allEntitlements() {
        return new ArrayList<>(entitlements.values());
    }

    private static String key(UserKey userKey, String collectionKey) {
        return userKey.value() + "\u0000" + collectionKey;
    }
}
