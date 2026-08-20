package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.AccessDecisionCache;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.profile.UserKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class RedisAccessDecisionCache implements AccessDecisionCache {

    private static final String PREFIX = "entitlement-access:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAccessDecisionCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AccessDecision> find(
            UserKey userKey,
            boolean administrator,
            String resourceKey
    ) {
        String value = redisTemplate.opsForValue().get(decisionKey(userKey, administrator, resourceKey));
        if (value == null) {
            return Optional.empty();
        }
        try {
            CachedDecision cached = objectMapper.readValue(value, CachedDecision.class);
            return Optional.of(new AccessDecision(
                    cached.allowed(),
                    AccessDecisionReason.valueOf(cached.reason()),
                    cached.collectionKey(),
                    cached.evaluatedAt()));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("invalid access decision cache entry", exception);
        }
    }

    @Override
    public void put(
            UserKey userKey,
            boolean administrator,
            String resourceKey,
            AccessDecision decision,
            Duration ttl
    ) {
        String decisionKey = decisionKey(userKey, administrator, resourceKey);
        String indexKey = indexKey(userKey, decision.collectionKey());
        try {
            String value = objectMapper.writeValueAsString(new CachedDecision(
                    decision.allowed(),
                    decision.reason().name(),
                    decision.collectionKey(),
                    decision.evaluatedAt()));
            redisTemplate.opsForValue().set(decisionKey, value, ttl);
            redisTemplate.opsForSet().add(indexKey, decisionKey);
            redisTemplate.expire(indexKey, ttl.plusSeconds(5));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("could not encode access decision cache entry", exception);
        }
    }

    @Override
    public void invalidate(UserKey userKey, String collectionKey) {
        String indexKey = indexKey(userKey, collectionKey);
        Set<String> decisionKeys = redisTemplate.opsForSet().members(indexKey);
        if (decisionKeys != null && !decisionKeys.isEmpty()) {
            redisTemplate.delete(decisionKeys);
        }
        redisTemplate.delete(indexKey);
    }

    private static String decisionKey(UserKey userKey, boolean administrator, String resourceKey) {
        return PREFIX + "decision:" + userKey.value() + ':' + administrator + ':' + resourceKey;
    }

    private static String indexKey(UserKey userKey, String collectionKey) {
        return PREFIX + "index:" + userKey.value() + ':' + collectionKey;
    }

    private record CachedDecision(
            boolean allowed,
            String reason,
            String collectionKey,
            Instant evaluatedAt
    ) {
    }
}
