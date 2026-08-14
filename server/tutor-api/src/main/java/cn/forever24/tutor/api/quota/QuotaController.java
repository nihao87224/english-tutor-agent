package cn.forever24.tutor.api.quota;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/quota")
public class QuotaController {

    private final DailyQuotaApplicationService dailyQuotaApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public QuotaController(
            DailyQuotaApplicationService dailyQuotaApplicationService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.dailyQuotaApplicationService = dailyQuotaApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @GetMapping
    public QuotaResponse quota() {
        return QuotaResponse.from(dailyQuotaApplicationService.currentQuota(currentUserKeyResolver.resolve(null)));
    }
}
