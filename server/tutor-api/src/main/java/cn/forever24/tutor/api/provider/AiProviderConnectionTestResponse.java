package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderConnectionTestResult;

public record AiProviderConnectionTestResponse(boolean success, long latencyMs, String error) {

    static AiProviderConnectionTestResponse from(AiProviderConnectionTestResult result) {
        return new AiProviderConnectionTestResponse(result.success(), result.latencyMs(), result.error());
    }
}
