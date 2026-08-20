package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ContentProvider;

import java.util.Optional;

public interface ContentProviderRepository {

    Optional<ContentProvider> findProvider(String providerCode);
}
