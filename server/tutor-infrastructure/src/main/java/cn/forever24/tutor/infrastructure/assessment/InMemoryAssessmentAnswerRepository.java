package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.AssessmentAnswerRepository;
import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.assessment.AssessmentAttemptEvidence;
import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;
import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.assessment.InitialAssessmentProfileGenerator;
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.UserKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAssessmentAnswerRepository implements AssessmentAnswerRepository, AssessmentResultRepository {

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<String, AssessmentAnswerReceipt> receipts = new ConcurrentHashMap<>();
    private final Map<String, AssessmentAttemptEvidence> attempts = new ConcurrentHashMap<>();
    private final Map<String, AssessmentResult> results = new ConcurrentHashMap<>();

    @Override
    public AssessmentAnswerReceipt saveObjectiveAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredObjectiveAnswer answer
    ) {
        validateAssessmentId(assessmentId);
        String key = userKey.value() + ":" + assessmentId + ":" + answer.itemId();
        return receipts.computeIfAbsent(key, ignored -> {
            attempts.put(key, new AssessmentAttemptEvidence(
                    answer.itemId(),
                    answer.score().correctness(),
                    answer.score().score(),
                    answer.score().evaluatorConfidence()));
            return new AssessmentAnswerReceipt("answer-" + sequence.incrementAndGet(), true);
        });
    }

    @Override
    public AssessmentAnswerReceipt saveOpenAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredOpenAnswer answer
    ) {
        validateAssessmentId(assessmentId);
        String key = userKey.value() + ":" + assessmentId + ":" + answer.itemId();
        return receipts.computeIfAbsent(key, ignored -> {
            attempts.put(key, new AssessmentAttemptEvidence(
                    answer.itemId(),
                    answer.evaluation().correctness(),
                    answer.evaluation().score(),
                    answer.evaluation().evaluatorConfidence()));
            return new AssessmentAnswerReceipt("answer-" + sequence.incrementAndGet(), true);
        });
    }

    @Override
    public Set<String> answeredItemIds(UserKey userKey, String assessmentId) {
        validateAssessmentId(assessmentId);
        String prefix = userKey.value() + ":" + assessmentId + ":";
        return receipts.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId) {
        validateAssessmentId(assessmentId);
        String resultKey = userKey.value() + ":" + assessmentId;
        return results.computeIfAbsent(resultKey, ignored -> {
            List<AssessmentAttemptEvidence> evidence = attemptsFor(userKey, assessmentId);
            if (evidence.isEmpty()) {
                throw new IllegalArgumentException("assessment has no submitted attempts");
            }
            return InitialAssessmentProfileGenerator.generate(assessmentId, evidence);
        });
    }

    @Override
    public AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId) {
        validateAssessmentId(assessmentId);
        AssessmentResult result = results.get(userKey.value() + ":" + assessmentId);
        if (result == null) {
            throw new IllegalArgumentException("assessment result was not found");
        }
        return result;
    }

    @Override
    public boolean hasCompletedInitialAssessmentResult(UserKey userKey) {
        String prefix = userKey.value() + ":";
        return results.keySet().stream().anyMatch(key -> key.startsWith(prefix));
    }

    private List<AssessmentAttemptEvidence> attemptsFor(UserKey userKey, String assessmentId) {
        String prefix = userKey.value() + ":" + assessmentId + ":";
        List<AssessmentAttemptEvidence> evidence = new ArrayList<>();
        for (Map.Entry<String, AssessmentAttemptEvidence> entry : attempts.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                evidence.add(entry.getValue());
            }
        }
        return evidence;
    }

    private void validateAssessmentId(String assessmentId) {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessmentId is required");
        }
    }
}
