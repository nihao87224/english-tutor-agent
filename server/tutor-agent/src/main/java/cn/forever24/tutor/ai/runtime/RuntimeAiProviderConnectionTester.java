package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConnectionTestResult;
import cn.forever24.tutor.application.provider.AiProviderConnectionTester;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

/** Performs an explicit, administrator-triggered minimal LLM request. */
public final class RuntimeAiProviderConnectionTester implements AiProviderConnectionTester {

    private static final String CONNECTION_TEST_PROMPT_VERSION = "provider-connection-test-v1";
    private static final String CONNECTION_TEST_SCHEMA_VERSION = "none-v1";

    private final ObjectMapper objectMapper;

    public RuntimeAiProviderConnectionTester(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProviderConnectionTestResult test(ActiveAiProviderConfiguration configuration) {
        long startedAt = System.nanoTime();
        try {
            ChatProvider provider = RuntimeOpenAiChatProvider.forConfiguration(configuration, objectMapper);
            provider.stream(ChatProviderRequest.structured(
                    UUID.randomUUID().toString(),
                    CONNECTION_TEST_PROMPT_VERSION,
                    CONNECTION_TEST_SCHEMA_VERSION,
                    "Reply with the single word OK."));
            return AiProviderConnectionTestResult.success(elapsedMillis(startedAt));
        } catch (AiProviderException exception) {
            return AiProviderConnectionTestResult.failure(elapsedMillis(startedAt), errorCode(exception.errorType()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return AiProviderConnectionTestResult.failure(elapsedMillis(startedAt), "INVALID_CONFIGURATION");
        }
    }

    private static String errorCode(AiProviderErrorType errorType) {
        return switch (errorType) {
            case AUTHENTICATION_FAILED -> "INVALID_API_KEY";
            case TIMEOUT -> "TIMEOUT";
            case PROVIDER_UNAVAILABLE -> "PROVIDER_UNAVAILABLE";
            case VALIDATION_ERROR, INVALID_OUTPUT -> "INVALID_CONFIGURATION";
            case UNKNOWN -> "CONNECTION_FAILED";
        };
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
