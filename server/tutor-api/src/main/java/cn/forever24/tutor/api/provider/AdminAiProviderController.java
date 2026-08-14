package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai-providers")
public class AdminAiProviderController {

    private final AiProviderConfigurationApplicationService applicationService;

    public AdminAiProviderController(AiProviderConfigurationApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<AiProviderResponse> listProviders() {
        return applicationService.listProviders().stream()
                .map(AiProviderResponse::from)
                .toList();
    }

    @GetMapping("/{providerCode}")
    public AiProviderResponse getProvider(@PathVariable String providerCode) {
        return AiProviderResponse.from(applicationService.getProvider(providerCode));
    }

    @PutMapping("/{providerCode}")
    public AiProviderResponse saveProvider(
            @PathVariable String providerCode,
            @RequestBody AiProviderUpdateRequest request
    ) {
        AiProviderConfiguration configuration = applicationService.saveProvider(
                providerCode,
                request.providerType() == null ? null : request.providerType().name(),
                request.displayName(),
                request.enabled(),
                request.defaultLlm(),
                request.defaultAsr(),
                request.defaultTts(),
                request.baseUrl() == null ? null : request.baseUrl().toString(),
                request.llmModel(),
                request.asrModel(),
                request.ttsModel(),
                request.ttsVoice(),
                Duration.ofSeconds(request.timeoutSeconds() == null ? 30 : request.timeoutSeconds()));
        return AiProviderResponse.from(configuration);
    }

    @PutMapping("/{providerCode}/secret")
    public AiProviderResponse replaceApiKey(
            @PathVariable String providerCode,
            @RequestBody AiProviderSecretRequest request,
            Authentication authentication
    ) {
        AiProviderConfiguration configuration = applicationService.replaceApiKey(
                providerCode,
                request.apiKey(),
                Long.parseLong(authentication.getName()));
        return AiProviderResponse.from(configuration);
    }
}
