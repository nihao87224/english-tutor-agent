package cn.forever24.tutor.api.admin;

import java.util.Set;

public record AdminUserRolesRequest(Set<String> roles) {
}
