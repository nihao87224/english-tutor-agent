package cn.forever24.tutor.api.auth;

import cn.forever24.tutor.application.auth.AuthApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AuthApplicationService authApplicationService;

    public MeController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @GetMapping
    public UserResponse me(Authentication authentication) {
        long userId = Long.parseLong(authentication.getName());
        return UserResponse.from(authApplicationService.currentUser(userId));
    }
}
