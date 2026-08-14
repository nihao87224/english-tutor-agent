package cn.forever24.tutor.infrastructure.auth;

import java.util.Set;

public record VerifiedAccessToken(long userId, Set<String> authorities, Set<String> roles, long authVersion) {
}
