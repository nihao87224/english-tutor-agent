package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;

import java.util.List;
import java.util.Optional;

public interface EntitlementRepository {

    Optional<Entitlement> find(UserKey userKey, String collectionKey);

    Optional<Entitlement> findForUpdate(UserKey userKey, String collectionKey);

    List<Entitlement> findForUser(UserKey userKey);

    void insert(Entitlement entitlement);

    void update(Entitlement entitlement, long expectedVersion);
}
