package cn.forever24.tutor.planning.policy;

public record PedagogicalPolicyVersion(String value) {

    public static final PedagogicalPolicyVersion V2_P0_1 = new PedagogicalPolicyVersion("V2-P0-1");

    public PedagogicalPolicyVersion {
        if (value == null || value.isBlank() || value.strip().length() > 32) {
            throw new IllegalArgumentException("valid policy version is required");
        }
        value = value.strip();
    }
}
