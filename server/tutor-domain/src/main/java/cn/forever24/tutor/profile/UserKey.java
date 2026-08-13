package cn.forever24.tutor.profile;

import java.util.Objects;
import java.util.regex.Pattern;

public record UserKey(String value) {

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public UserKey {
        Objects.requireNonNull(value, "value must not be null");
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("user key must be 1-64 letters, digits, dot, underscore or dash");
        }
    }
}
