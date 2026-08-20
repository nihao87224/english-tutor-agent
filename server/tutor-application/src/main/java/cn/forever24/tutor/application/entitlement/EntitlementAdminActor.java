package cn.forever24.tutor.application.entitlement;

import java.util.Set;

public record EntitlementAdminActor(long userId, Set<String> permissions) {

    public static final String MANAGE_PERMISSION = "ENTITLEMENT_MANAGE";

    public EntitlementAdminActor {
        if (userId <= 0) {
            throw new IllegalArgumentException("actor userId must be positive");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public void requireManagePermission() {
        if (!permissions.contains(MANAGE_PERMISSION)) {
            throw EntitlementApplicationException.forbidden(
                    "ENTITLEMENT_PERMISSION_REQUIRED",
                    "ENTITLEMENT_MANAGE permission is required");
        }
    }
}
