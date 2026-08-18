package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/** Logs a clear, non-fatal operator action when no usable LLM is configured. */
public final class AiProviderStartupCheck implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiProviderStartupCheck.class);

    private final AiProviderConfigurationApplicationService configurationService;

    public AiProviderStartupCheck(AiProviderConfigurationApplicationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!configurationService.hasConfiguredDefaultLlmProvider()) {
            LOGGER.warn("AI Provider: NOT CONFIGURED. Configure an enabled default LLM and API key in Admin -> AI Provider Management.");
        }
    }
}
