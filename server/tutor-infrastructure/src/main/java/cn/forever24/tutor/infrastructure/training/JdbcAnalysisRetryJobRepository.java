package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.AnalysisRetryJob;
import cn.forever24.tutor.application.training.AnalysisRetryJobRepository;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

public final class JdbcAnalysisRetryJobRepository implements AnalysisRetryJobRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcAnalysisRetryJobRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate); }

    @Override
    public void schedule(UserKey userKey, String sessionId, String attemptId, String errorCode, Instant nextRunAt) {
        Long attemptDbId = jdbcTemplate.queryForObject("""
                SELECT ta.id FROM task_attempt ta JOIN training_session ts ON ts.id = ta.session_id
                JOIN app_user u ON u.id = ta.user_id WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ?
                """, Long.class, userKey.value(), sessionId, attemptId);
        jdbcTemplate.update("""
                INSERT INTO analysis_retry_job
                (job_key, attempt_id, job_type, status, attempt_count, next_run_at_utc, last_error_code, created_at_utc, updated_at_utc, version)
                VALUES (?, ?, 'SPEAKING_ANALYSIS', 'PENDING', 1, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 1)
                ON DUPLICATE KEY UPDATE status = IF(status = 'COMPLETED', status, 'PENDING'),
                    next_run_at_utc = IF(status = 'COMPLETED', next_run_at_utc, VALUES(next_run_at_utc)),
                    last_error_code = IF(status = 'COMPLETED', last_error_code, VALUES(last_error_code)),
                    updated_at_utc = VALUES(updated_at_utc), version = version + 1
                """, sha256(attemptId), attemptDbId, utc(nextRunAt), errorCode);
    }

    @Override
    public List<AnalysisRetryJob> claimDue(Instant now, int limit, String workerId) {
        List<Row> due = jdbcTemplate.query("""
                SELECT arj.id, u.user_key, ts.session_key, ta.attempt_key, arj.attempt_count
                FROM analysis_retry_job arj JOIN task_attempt ta ON ta.id = arj.attempt_id
                JOIN training_session ts ON ts.id = ta.session_id JOIN app_user u ON u.id = ta.user_id
                WHERE arj.status = 'PENDING' AND arj.next_run_at_utc <= ? ORDER BY arj.next_run_at_utc LIMIT ?
                """, (rs, row) -> new Row(rs.getLong("id"), new UserKey(rs.getString("user_key")),
                rs.getString("session_key"), rs.getString("attempt_key"), rs.getInt("attempt_count")), utc(now), limit);
        return due.stream().filter(row -> jdbcTemplate.update("""
                UPDATE analysis_retry_job SET status = 'RUNNING', lease_owner = ?, lease_until_utc = ?,
                updated_at_utc = UTC_TIMESTAMP(6), version = version + 1 WHERE id = ? AND status = 'PENDING'
                """, workerId, utc(now.plusSeconds(60)), row.id()) == 1)
                .map(row -> new AnalysisRetryJob(row.userKey(), row.sessionId(), row.attemptId(), row.attemptCount())).toList();
    }

    @Override public void complete(AnalysisRetryJob job) { update(job, "COMPLETED", null, null); }
    @Override public void reschedule(AnalysisRetryJob job, String errorCode, Instant nextRunAt) { update(job, "PENDING", errorCode, nextRunAt); }
    @Override public void failFinal(AnalysisRetryJob job, String errorCode) { update(job, "FAILED_FINAL", errorCode, null); }

    private void update(AnalysisRetryJob job, String status, String errorCode, Instant next) {
        jdbcTemplate.update("""
                UPDATE analysis_retry_job arj JOIN task_attempt ta ON ta.id = arj.attempt_id
                JOIN training_session ts ON ts.id = ta.session_id JOIN app_user u ON u.id = ta.user_id
                SET arj.status = ?, arj.attempt_count = CASE WHEN ? = 'PENDING' THEN arj.attempt_count + 1 ELSE arj.attempt_count END,
                arj.next_run_at_utc = COALESCE(?, arj.next_run_at_utc), arj.last_error_code = ?, arj.lease_owner = NULL,
                arj.lease_until_utc = NULL, arj.updated_at_utc = UTC_TIMESTAMP(6), arj.version = arj.version + 1
                WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ?
                """, status, status, next == null ? null : utc(next), errorCode,
                job.userKey().value(), job.sessionId(), job.attemptId());
    }
    private static LocalDateTime utc(Instant value) { return LocalDateTime.ofInstant(value, ZoneOffset.UTC); }
    private static String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
    private record Row(long id, UserKey userKey, String sessionId, String attemptId, int attemptCount) { }
}
