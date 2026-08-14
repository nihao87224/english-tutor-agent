package cn.forever24.tutor.application.provider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiProviderConfigurationRepository {

    List<AiProviderConfiguration> list();

    Optional<AiProviderConfiguration> findByCode(String providerCode);

    ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose);

    AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now);

    AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now);
}
