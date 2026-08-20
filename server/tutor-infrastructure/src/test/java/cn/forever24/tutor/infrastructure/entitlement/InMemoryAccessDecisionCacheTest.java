package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccessDecisionCacheTest {

    @Test
    void expiresByShortTtlAndInvalidatesByUserCollection() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-20T00:00:00Z"));
        InMemoryAccessDecisionCache cache = new InMemoryAccessDecisionCache(clock);
        UserKey userKey = new UserKey("usr_learner");
        AccessDecision decision = AccessDecision.allow(
                AccessDecisionReason.ALLOWED_ADMIN_GRANTED, "private", clock.instant());

        cache.put(userKey, false, "resource-1", decision, Duration.ofSeconds(30));
        assertTrue(cache.find(userKey, false, "resource-1").isPresent());
        cache.invalidate(userKey, "private");
        assertTrue(cache.find(userKey, false, "resource-1").isEmpty());

        cache.put(userKey, false, "resource-1", decision, Duration.ofSeconds(30));
        clock.advance(Duration.ofSeconds(30));
        assertTrue(cache.find(userKey, false, "resource-1").isEmpty());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
