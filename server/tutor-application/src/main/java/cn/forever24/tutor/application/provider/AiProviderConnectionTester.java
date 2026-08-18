package cn.forever24.tutor.application.provider;

/** Outbound port used to validate a fully resolved provider configuration. */
public interface AiProviderConnectionTester {

    AiProviderConnectionTestResult test(ActiveAiProviderConfiguration configuration);
}
