package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessPolicy;
import cn.forever24.tutor.entitlement.AccessRequest;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EntitlementApplicationService {

    private final EntitlementRepository entitlementRepository;
    private final ResourceAccessTargetRepository targetRepository;
    private final EntitlementAuditPort auditPort;
    private final EntitlementTransactionOperations transactions;
    private final AccessDecisionCache decisionCache;
    private final EntitlementKeyGenerator keyGenerator;
    private final AccessPolicy accessPolicy;
    private final Clock clock;
    private final Duration decisionCacheTtl;

    public EntitlementApplicationService(
            EntitlementRepository entitlementRepository,
            ResourceAccessTargetRepository targetRepository,
            EntitlementAuditPort auditPort,
            EntitlementTransactionOperations transactions,
            AccessDecisionCache decisionCache,
            EntitlementKeyGenerator keyGenerator,
            AccessPolicy accessPolicy,
            Clock clock,
            Duration decisionCacheTtl
    ) {
        this.entitlementRepository = Objects.requireNonNull(entitlementRepository);
        this.targetRepository = Objects.requireNonNull(targetRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.transactions = Objects.requireNonNull(transactions);
        this.decisionCache = Objects.requireNonNull(decisionCache);
        this.keyGenerator = Objects.requireNonNull(keyGenerator);
        this.accessPolicy = Objects.requireNonNull(accessPolicy);
        this.clock = Objects.requireNonNull(clock);
        this.decisionCacheTtl = Objects.requireNonNull(decisionCacheTtl);
        if (decisionCacheTtl.isNegative() || decisionCacheTtl.isZero()) {
            throw new IllegalArgumentException("decisionCacheTtl must be positive");
        }
    }

    public EntitlementMutationResult grant(EntitlementAdminActor actor, GrantEntitlementCommand command) {
        requireActorAndCommand(actor, command);
        actor.requireManagePermission();
        Instant now = clock.instant();
        if (!targetRepository.collectionExists(command.collectionKey())) {
            throw EntitlementApplicationException.notFound(
                    "COLLECTION_NOT_FOUND", "collection was not found: " + command.collectionKey());
        }
        EntitlementMutationResult result = transactions.execute(() -> {
            Optional<Entitlement> current = entitlementRepository.findForUpdate(
                    command.userKey(), command.collectionKey());
            if (current.isEmpty()) {
                Entitlement created = Entitlement.grant(
                        keyGenerator.nextKey(),
                        command.userKey(),
                        command.collectionKey(),
                        actor.userId(),
                        now,
                        command.expiresAt(),
                        command.reason());
                entitlementRepository.insert(created);
                auditPort.append(actor.userId(), "ENTITLEMENT_GRANTED", created.entitlementKey(), null,
                        auditState(created), now);
                return new EntitlementMutationResult(created, EntitlementMutationOutcome.CREATED);
            }
            Entitlement previous = current.orElseThrow();
            Entitlement granted = previous.grant(actor.userId(), now, command.expiresAt(), command.reason());
            if (granted == previous) {
                return new EntitlementMutationResult(previous, EntitlementMutationOutcome.UNCHANGED);
            }
            entitlementRepository.update(granted, previous.version());
            auditPort.append(actor.userId(), "ENTITLEMENT_GRANTED", granted.entitlementKey(),
                    auditState(previous), auditState(granted), now);
            return new EntitlementMutationResult(granted, EntitlementMutationOutcome.UPDATED);
        });
        invalidateBestEffort(command.userKey(), command.collectionKey());
        return result;
    }

    public EntitlementMutationResult revoke(EntitlementAdminActor actor, RevokeEntitlementCommand command) {
        requireActorAndCommand(actor, command);
        actor.requireManagePermission();
        Instant now = clock.instant();
        EntitlementMutationResult result = transactions.execute(() -> {
            Entitlement previous = entitlementRepository.findForUpdate(command.userKey(), command.collectionKey())
                    .orElseThrow(() -> EntitlementApplicationException.notFound(
                            "ENTITLEMENT_NOT_FOUND", "entitlement was not found"));
            Entitlement revoked = previous.revoke(now, command.reason());
            if (revoked == previous) {
                return new EntitlementMutationResult(previous, EntitlementMutationOutcome.UNCHANGED);
            }
            entitlementRepository.update(revoked, previous.version());
            auditPort.append(actor.userId(), "ENTITLEMENT_REVOKED", revoked.entitlementKey(),
                    auditState(previous), auditState(revoked), now);
            return new EntitlementMutationResult(revoked, EntitlementMutationOutcome.UPDATED);
        });
        invalidateBestEffort(command.userKey(), command.collectionKey());
        return result;
    }

    public EntitlementMutationResult expire(
            EntitlementAdminActor actor,
            UserKey userKey,
            String collectionKey
    ) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(userKey, "userKey must not be null");
        actor.requireManagePermission();
        String normalizedCollectionKey = required(collectionKey, "collectionKey");
        Instant now = clock.instant();
        EntitlementMutationResult result = transactions.execute(() -> {
            Entitlement previous = entitlementRepository.findForUpdate(userKey, normalizedCollectionKey)
                    .orElseThrow(() -> EntitlementApplicationException.notFound(
                            "ENTITLEMENT_NOT_FOUND", "entitlement was not found"));
            Entitlement expired = previous.expire(now);
            if (expired == previous) {
                return new EntitlementMutationResult(previous, EntitlementMutationOutcome.UNCHANGED);
            }
            entitlementRepository.update(expired, previous.version());
            auditPort.append(actor.userId(), "ENTITLEMENT_EXPIRED", expired.entitlementKey(),
                    auditState(previous), auditState(expired), now);
            return new EntitlementMutationResult(expired, EntitlementMutationOutcome.UPDATED);
        });
        invalidateBestEffort(userKey, normalizedCollectionKey);
        return result;
    }

    public List<Entitlement> listForCurrentUser(UserKey currentUser) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        Instant now = clock.instant();
        return entitlementRepository.findForUser(currentUser).stream()
                .filter(entitlement -> entitlement.isActiveAt(now))
                .toList();
    }

    public AccessDecision decide(UserKey currentUser, boolean administrator, String resourceKey) {
        return decide(currentUser, administrator, resourceKey, true);
    }

    public AccessDecision decideAuthoritatively(
            UserKey currentUser,
            boolean administrator,
            String resourceKey
    ) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        String normalizedResourceKey = required(resourceKey, "resourceKey");
        return transactions.execute(() -> evaluateFromAuthoritativeRepositories(
                currentUser, administrator, normalizedResourceKey, true).decision());
    }

    private AccessDecision decide(
            UserKey currentUser,
            boolean administrator,
            String resourceKey,
            boolean cacheable
    ) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        String normalizedResourceKey = required(resourceKey, "resourceKey");
        if (cacheable) {
            try {
                Optional<AccessDecision> cached = decisionCache.find(
                        currentUser, administrator, normalizedResourceKey);
                if (cached.isPresent()) {
                    return cached.orElseThrow();
                }
            } catch (RuntimeException ignored) {
                // Redis is an optimization. The authoritative repositories remain the source of truth.
            }
        }

        DecisionEvaluation evaluation = evaluateFromAuthoritativeRepositories(
                currentUser, administrator, normalizedResourceKey, false);
        AccessDecision decision = evaluation.decision();
        if (cacheable) {
            Duration ttl = effectiveTtl(evaluation.entitlement(), decision.evaluatedAt());
            try {
                decisionCache.put(currentUser, administrator, normalizedResourceKey, decision, ttl);
            } catch (RuntimeException ignored) {
                // Cache write failures must not change an authoritative access decision.
            }
        }
        return decision;
    }

    private DecisionEvaluation evaluateFromAuthoritativeRepositories(
            UserKey currentUser,
            boolean administrator,
            String normalizedResourceKey,
            boolean lockEntitlement
    ) {
        ResourceAccessTarget target = targetRepository.findByResourceKey(normalizedResourceKey)
                .orElseThrow(() -> EntitlementApplicationException.notFound(
                        "RESOURCE_NOT_FOUND", "resource was not found: " + normalizedResourceKey));
        Optional<Entitlement> entitlement = lockEntitlement
                ? entitlementRepository.findForUpdate(currentUser, target.collection().collectionKey())
                : entitlementRepository.find(currentUser, target.collection().collectionKey());
        Instant now = clock.instant();
        AccessDecision decision = accessPolicy.decide(new AccessRequest(
                currentUser,
                administrator,
                target.resource(),
                target.collection(),
                entitlement,
                now));
        return new DecisionEvaluation(decision, entitlement);
    }

    private Duration effectiveTtl(Optional<Entitlement> entitlement, Instant now) {
        if (entitlement.isEmpty() || entitlement.orElseThrow().expiresAt() == null) {
            return decisionCacheTtl;
        }
        Duration untilExpiry = Duration.between(now, entitlement.orElseThrow().expiresAt());
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            return Duration.ofMillis(1);
        }
        return untilExpiry.compareTo(decisionCacheTtl) < 0 ? untilExpiry : decisionCacheTtl;
    }

    private void invalidateBestEffort(UserKey userKey, String collectionKey) {
        try {
            decisionCache.invalidate(userKey, collectionKey);
        } catch (RuntimeException ignored) {
            // A failed invalidation cannot roll back an already committed authorization fact.
        }
    }

    private static void requireActorAndCommand(Object actor, Object command) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(command, "command must not be null");
    }

    private static String auditState(Entitlement entitlement) {
        return entitlement.status() + ":v" + entitlement.version();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private record DecisionEvaluation(AccessDecision decision, Optional<Entitlement> entitlement) {
    }
}
