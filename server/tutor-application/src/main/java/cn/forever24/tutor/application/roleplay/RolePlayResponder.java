package cn.forever24.tutor.application.roleplay;

@FunctionalInterface
public interface RolePlayResponder {
    RolePlayResponse respond(RolePlayResponseContext context);
}
