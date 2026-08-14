package cn.forever24.tutor.infrastructure.auth;

import cn.forever24.tutor.application.auth.AccessTokenIssuer;
import cn.forever24.tutor.application.auth.AuthException;
import cn.forever24.tutor.application.auth.AuthenticatedUser;
import cn.forever24.tutor.application.auth.IssuedAccessToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HmacJwtAccessTokenService implements AccessTokenIssuer {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public HmacJwtAccessTokenService(String secret, Duration ttl, Clock clock, ObjectMapper objectMapper) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT signing secret must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl == null ? DEFAULT_TTL : ttl;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public IssuedAccessToken issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.userKey());
        claims.put("uid", user.userId());
        claims.put("roles", user.roles());
        claims.put("authorities", user.authorities());
        claims.put("auth_version", user.authVersion());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        String signingInput = encodeJson(header) + "." + encodeJson(claims);
        return new IssuedAccessToken(signingInput + "." + sign(signingInput), ttl.toSeconds());
    }

    public VerifiedAccessToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
        String signingInput = segments[0] + "." + segments[1];
        if (!constantTimeEquals(sign(signingInput), segments[2])) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
        Map<String, Object> claims = decodeClaims(segments[1]);
        long expiresAt = numberClaim(claims, "exp");
        if (expiresAt <= clock.instant().getEpochSecond()) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
        return new VerifiedAccessToken(
                numberClaim(claims, "uid"),
                stringSetClaim(claims, "authorities"),
                stringSetClaim(claims, "roles"),
                numberClaim(claims, "auth_version"));
    }

    private String encodeJson(Object value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode JWT", exception);
        }
    }

    private Map<String, Object> decodeClaims(String encodedClaims) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(encodedClaims), new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private static long numberClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw AuthException.unauthorized("INVALID_ACCESS_TOKEN", "Access token is invalid");
    }

    private static Set<String> stringSetClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
