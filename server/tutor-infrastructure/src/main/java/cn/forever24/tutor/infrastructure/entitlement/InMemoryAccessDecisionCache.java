package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.AccessDecisionCache;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.profile.UserKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAccessDecisionCache implements AccessDecisionCache {

    private final Map<Key, Entry> entries = new HashMap<>();
    private final Clock clock;

    public InMemoryAccessDecisionCache(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized Optional<AccessDecision> find(
            UserKey userKey,
            boolean administrator,
            String resourceKey
    ) {
        Key key = new Key(userKey.value(), administrator, resourceKey);
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.decision());
    }

    @Override
    public synchronized void put(
            UserKey userKey,
            boolean administrator,
            String resourceKey,
            AccessDecision decision,
            Duration ttl
    ) {
        entries.put(
                new Key(userKey.value(), administrator, resourceKey),
                new Entry(decision, clock.instant().plus(ttl)));
    }

    @Override
    public synchronized void invalidate(UserKey userKey, String collectionKey) {
        entries.entrySet().removeIf(entry -> entry.getKey().userKey().equals(userKey.value())
                && entry.getValue().decision().collectionKey().equals(collectionKey));
    }

    private record Key(String userKey, boolean administrator, String resourceKey) {
    }

    private record Entry(AccessDecision decision, Instant expiresAt) {
    }
}
