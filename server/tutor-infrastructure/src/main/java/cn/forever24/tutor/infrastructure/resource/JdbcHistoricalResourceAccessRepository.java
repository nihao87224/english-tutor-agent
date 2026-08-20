package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.HistoricalResourceAccessRepository;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

public final class JdbcHistoricalResourceAccessRepository implements HistoricalResourceAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcHistoricalResourceAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public boolean hasSessionOrEvidenceReference(UserKey userKey, String resourceKey, String semanticVersion) {
        Integer references = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM training_session session
                        JOIN app_user learner ON learner.id = session.user_id
                        JOIN learning_resource_version version ON version.id = session.resource_version_id
                        JOIN learning_resource resource ON resource.id = version.resource_id
                        WHERE learner.user_key = ? AND learner.status = 'ACTIVE'
                          AND session.type = 'SCENARIO_LESSON'
                          AND resource.resource_key = ? AND version.semantic_version = ?
                        """,
                Integer.class, userKey.value(), resourceKey, semanticVersion);
        return references != null && references > 0;
    }
}
