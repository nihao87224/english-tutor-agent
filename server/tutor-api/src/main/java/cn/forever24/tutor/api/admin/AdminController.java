package cn.forever24.tutor.api.admin;

import cn.forever24.tutor.application.admin.AdminApplicationService;
import cn.forever24.tutor.application.admin.AdminAuditEntry;
import cn.forever24.tutor.application.admin.AdminDashboardSummary;
import cn.forever24.tutor.application.admin.AdminException;
import cn.forever24.tutor.application.admin.AdminPage;
import cn.forever24.tutor.application.admin.AdminQuotaState;
import cn.forever24.tutor.application.admin.AdminSystemSetting;
import cn.forever24.tutor.application.admin.AdminUserDetail;
import cn.forever24.tutor.application.admin.AdminUserSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminApplicationService applicationService;

    public AdminController(AdminApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public AdminDashboardSummary dashboard() {
        return applicationService.dashboard();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public AdminPage<AdminUserSummary> searchUsers(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return applicationService.searchUsers(q, status, role, page, size);
    }

    @GetMapping("/users/{userKey}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public AdminUserDetail getUser(@PathVariable("userKey") String userKey) {
        return applicationService.getUser(userKey);
    }

    @PatchMapping("/users/{userKey}/status")
    @PreAuthorize("hasAuthority('USER_STATUS_MANAGE')")
    public AdminUserDetail updateUserStatus(
            @PathVariable("userKey") String userKey,
            @RequestBody AdminUserStatusRequest request,
            Authentication authentication
    ) {
        request = requireBody(request);
        return applicationService.updateUserStatus(userKey, request.status(), actorUserId(authentication));
    }

    @PutMapping("/users/{userKey}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_MANAGE')")
    public AdminUserDetail replaceUserRoles(
            @PathVariable("userKey") String userKey,
            @RequestBody AdminUserRolesRequest request,
            Authentication authentication
    ) {
        request = requireBody(request);
        return applicationService.replaceUserRoles(userKey, request.roles(), actorUserId(authentication));
    }

    @PutMapping("/users/{userKey}/quota-policy")
    @PreAuthorize("hasAuthority('USER_QUOTA_MANAGE')")
    public AdminQuotaState updateQuotaPolicy(
            @PathVariable("userKey") String userKey,
            @RequestBody AdminQuotaPolicyRequest request,
            Authentication authentication
    ) {
        request = requireBody(request);
        return applicationService.updateQuotaPolicy(
                userKey,
                request.dailyLimitOverride(),
                request.unlimited(),
                actorUserId(authentication));
    }

    @PostMapping("/users/{userKey}/quota/reset-today")
    @PreAuthorize("hasAuthority('USER_QUOTA_MANAGE')")
    public AdminQuotaState resetTodayQuota(@PathVariable("userKey") String userKey, Authentication authentication) {
        return applicationService.resetTodayQuota(userKey, actorUserId(authentication));
    }

    @PostMapping("/users/{userKey}/quota/bonus")
    @PreAuthorize("hasAuthority('USER_QUOTA_MANAGE')")
    public AdminQuotaState addQuotaBonus(
            @PathVariable("userKey") String userKey,
            @RequestBody AdminQuotaBonusRequest request,
            Authentication authentication
    ) {
        request = requireBody(request);
        return applicationService.addQuotaBonus(userKey, request.bonus(), actorUserId(authentication));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_READ')")
    public List<AdminSystemSetting> listSettings() {
        return applicationService.listSettings();
    }

    @PutMapping("/settings/{key}")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_MANAGE')")
    public AdminSystemSetting updateSetting(
            @PathVariable("key") String key,
            @RequestBody AdminSystemSettingRequest request,
            Authentication authentication
    ) {
        request = requireBody(request);
        return applicationService.updateSetting(
                key,
                request.value(),
                request.valueType(),
                request.description(),
                actorUserId(authentication));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public AdminPage<AdminAuditEntry> listAudit(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return applicationService.listAudit(page, size);
    }

    private static long actorUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private static <T> T requireBody(T request) {
        if (request == null) {
            throw AdminException.invalid("request body is required");
        }
        return request;
    }
}
