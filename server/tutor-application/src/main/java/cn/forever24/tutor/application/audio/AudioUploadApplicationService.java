package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AudioUploadApplicationService {
    public static final long MAX_BYTES = 50L * 1024 * 1024;
    public static final long MAX_DURATION_MS = 600_000;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/webm", "audio/wav", "audio/x-wav", "audio/mp4", "audio/m4a", "audio/ogg", "audio/mpeg");
    private static final Set<String> ALLOWED_PURPOSES = Set.of(
            "LESSON_ATTEMPT", "ASSESSMENT", "TRAINING", "IELTS");

    private final AudioAssetRepository repository;
    private final PrivateAudioObjectStorage objectStorage;
    private final UserProfileRepository userProfileRepository;
    private final AudioAssetKeyGenerator keyGenerator;
    private final Clock clock;

    public AudioUploadApplicationService(
            AudioAssetRepository repository,
            PrivateAudioObjectStorage objectStorage,
            UserProfileRepository userProfileRepository,
            AudioAssetKeyGenerator keyGenerator,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.keyGenerator = Objects.requireNonNull(keyGenerator);
        this.clock = Objects.requireNonNull(clock);
    }

    public AudioUploadResult upload(String userKeyValue, UploadAudioCommand command, String idempotencyKey) {
        UserKey userKey = new UserKey(userKeyValue);
        String key = required(idempotencyKey, "Idempotency-Key");
        if (key.length() > 128) throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        Objects.requireNonNull(command, "command is required");
        byte[] content = command.content();
        validate(command, content);
        String digest = sha256(content);
        if (command.claimedSha256() != null && !command.claimedSha256().isBlank()
                && !normalizeHash(command.claimedSha256()).equals(digest)) {
            throw new IllegalArgumentException("sha256 does not match uploaded content");
        }
        String requestHash = sha256((normalizeMime(command.mimeType()) + "|" + command.durationMs() + "|"
                + command.purpose().strip() + "|" + digest).getBytes(StandardCharsets.UTF_8));
        var replay = repository.findByIdempotencyKey(userKey, key);
        if (replay.isPresent()) {
            if (!replay.orElseThrow().requestHash().equals(requestHash)) {
                throw AudioAssetApplicationException.idempotencyConflict();
            }
            return new AudioUploadResult(replay.orElseThrow().asset(), true);
        }

        Instant now = clock.instant();
        PrivacySettings privacy = userProfileRepository.getPrivacySettings(userKey);
        RawContentRetention retention = privacy.rawAudioRetention();
        Instant deleteAfter = retention == RawContentRetention.PROCESS_ONLY
                ? now.plus(Duration.ofHours(24))
                : privacy.rawAudioRetentionDays() > 0 ? now.plus(Duration.ofDays(privacy.rawAudioRetentionDays())) : null;
        String assetId = "usr_audio_" + keyGenerator.nextKey();
        String month = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC).format(now);
        String objectKey = "user-recordings/" + userKey.value() + "/" + month + "/" + assetId + extension(command.mimeType());
        UserAudioAsset asset = new UserAudioAsset(
                assetId, objectKey, command.purpose().strip(), normalizeMime(command.mimeType()), content.length,
                command.durationMs(), "sha256:" + digest, AudioAssetStatus.READY, retention, deleteAfter, now);
        try {
            objectStorage.put(objectKey, content);
        } catch (RuntimeException exception) {
            throw AudioAssetApplicationException.storageUnavailable();
        }
        try {
            repository.insert(userKey, asset, key, requestHash);
        } catch (RuntimeException exception) {
            try { objectStorage.delete(objectKey); } catch (RuntimeException ignored) { }
            var concurrentReplay = repository.findByIdempotencyKey(userKey, key);
            if (concurrentReplay.isPresent() && concurrentReplay.orElseThrow().requestHash().equals(requestHash)) {
                return new AudioUploadResult(concurrentReplay.orElseThrow().asset(), true);
            }
            throw exception;
        }
        return new AudioUploadResult(asset, false);
    }

    private static void validate(UploadAudioCommand command, byte[] content) {
        String purpose = required(command.purpose(), "purpose");
        if (!ALLOWED_PURPOSES.contains(purpose)) throw new IllegalArgumentException("unsupported audio purpose");
        String mime = normalizeMime(command.mimeType());
        if (!ALLOWED_TYPES.contains(mime)) throw new IllegalArgumentException("unsupported audio format");
        if (content == null || content.length == 0) throw new IllegalArgumentException("audio file is required");
        if (content.length > MAX_BYTES) throw new IllegalArgumentException("audio file must not exceed 50 MB");
        if (command.durationMs() < 100 || command.durationMs() > MAX_DURATION_MS) {
            throw new IllegalArgumentException("durationMs must be between 100 and 600000");
        }
    }

    private static String normalizeMime(String value) {
        return required(value, "mimeType").split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHash(String value) {
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return normalized.startsWith("sha256:") ? normalized.substring(7) : normalized;
    }

    private static String extension(String mime) {
        return switch (normalizeMime(mime)) {
            case "audio/webm" -> ".webm";
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/mp4", "audio/m4a" -> ".m4a";
            case "audio/ogg" -> ".ogg";
            default -> ".mp3";
        };
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
