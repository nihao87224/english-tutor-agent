package cn.forever24.tutor;

import com.jayway.jsonpath.JsonPath;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.conversation.ConversationStreamEvent;
import cn.forever24.tutor.application.conversation.CorrectionAnalysisContext;
import cn.forever24.tutor.application.conversation.CorrectionAnalyzer;
import cn.forever24.tutor.application.conversation.CorrectionSeverity;
import cn.forever24.tutor.application.conversation.CorrectionSuggestion;
import cn.forever24.tutor.application.conversation.CorrectionSuggestionStyle;
import cn.forever24.tutor.application.conversation.LayeredCorrectionItem;
import cn.forever24.tutor.application.conversation.LayeredCorrectionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final Map<String, String> accessTokensByScenarioUser = new ConcurrentHashMap<>();

    @TestConfiguration
    static class AiStubConfiguration {

        @Bean
        @Primary
        ConversationReplyStreamer testConversationReplyStreamer(CorrectionAnalyzer correctionAnalyzer) {
            return context -> List.of(
                    ConversationStreamEvent.status(1, "THINKING", "Understanding your message..."),
                    ConversationStreamEvent.textDelta(2, "This is a real-provider-shaped response."),
                    ConversationStreamEvent.correctionReady(3, correctionAnalyzer.analyze(new CorrectionAnalysisContext(
                            context.session(),
                            context.currentTask(),
                            context.message()))),
                    ConversationStreamEvent.done(4, "trace-test", "openai", "test-model"));
        }

        @Bean
        @Primary
        CorrectionAnalyzer testCorrectionAnalyzer() {
            return context -> new LayeredCorrectionResult(
                    true,
                    List.of(new LayeredCorrectionItem(
                            "very like",
                            "really like",
                            "word_order",
                            CorrectionSeverity.MEDIUM,
                            "Use 'really' before verbs like 'like'.",
                            false,
                            true,
                            List.of(new CorrectionSuggestion("I really like it.", CorrectionSuggestionStyle.NEUTRAL)))),
                    "Good communication. Focus on these small fixes while continuing the exchange.",
                    "correction-analyzer-v1",
                    "correction-result-v1",
                    "trace-test",
                    "openai",
                    "test-model");
        }
    }

    @Test
    void restoresGoalProgressForUserWithoutProfile() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer("new-integration-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("GOAL"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void savesPrimaryGoalAndRestoresOnboardingProgress() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("progress-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"WORKPLACE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryGoal").value("WORKPLACE"));

        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer("progress-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("PREFERENCES"));
    }

    @Test
    void savesPreferencesAndRestoresSelfAssessmentProgress() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("integration-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"WORKPLACE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer("integration-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 30,
                                  "correctionStyle": "LIGHT",
                                  "reminderEnabled": true,
                                  "saveRawText": false,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyMinutes").value(30))
                .andExpect(jsonPath("$.correctionStyle").value("LIGHT"));

        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer("integration-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("SELF_ASSESSMENT"));
    }

    @Test
    void readsAndUpdatesPrivacySettings() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("privacy-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"GENERAL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/settings/privacy")
                        .header(HttpHeaders.AUTHORIZATION, bearer("privacy-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saveRawText").value(true))
                .andExpect(jsonPath("$.saveRawAudio").value(true));

        mockMvc.perform(put("/api/v1/settings/privacy")
                        .header(HttpHeaders.AUTHORIZATION, bearer("privacy-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "saveRawText": false,
                                  "saveRawAudio": false,
                                  "rawAudioRetentionDays": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saveRawText").value(false))
                .andExpect(jsonPath("$.saveRawAudio").value(false))
                .andExpect(jsonPath("$.rawAudioRetentionDays").value(7));
    }

    @Test
    void submitsSelfAssessmentAndRestoresAssessmentProgress() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("self-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"IELTS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer("self-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 20,
                                  "correctionStyle": "STANDARD",
                                  "reminderEnabled": false,
                                  "saveRawText": true,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer("self-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "INTERMEDIATE",
                                  "speaking": "BASIC",
                                  "reading": "UPPER_INTERMEDIATE",
                                  "writing": "INTERMEDIATE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.selfAssessmentId").exists())
                .andExpect(jsonPath("$.estimatedBand").value("INTERMEDIATE"));

        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer("self-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("ASSESSMENT"));
    }

    @Test
    void startsAssessmentAfterSelfAssessmentAndReturnsExistingSession() throws Exception {
        String userKey = "assessment-user";

        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"WORKPLACE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 30,
                                  "correctionStyle": "STANDARD",
                                  "reminderEnabled": true,
                                  "saveRawText": true,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "INTERMEDIATE",
                                  "speaking": "BASIC",
                                  "reading": "INTERMEDIATE",
                                  "writing": "INTERMEDIATE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assessmentId").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.targetMinutes").value(10))
                .andExpect(jsonPath("$.estimatedRemainingMinutes").value(10));

        mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":15}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetMinutes").value(10));
    }

    @Test
    void submitsObjectiveAssessmentAnswer() throws Exception {
        String userKey = "answer-user";

        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"IELTS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 20,
                                  "correctionStyle": "STANDARD",
                                  "reminderEnabled": false,
                                  "saveRawText": true,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "INTERMEDIATE",
                                  "speaking": "INTERMEDIATE",
                                  "reading": "INTERMEDIATE",
                                  "writing": "INTERMEDIATE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":9}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assessments/assessment-1/answers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemId": "initial-reading-1",
                                  "answerType": "OPTION",
                                  "option": "B",
                                  "clientDurationMs": 1500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").exists())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    void completesAssessmentAndReturnsInitialProfileResult() throws Exception {
        String userKey = "result-user";

        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"WORKPLACE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 20,
                                  "correctionStyle": "STANDARD",
                                  "reminderEnabled": false,
                                  "saveRawText": true,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "INTERMEDIATE",
                                  "speaking": "INTERMEDIATE",
                                  "reading": "INTERMEDIATE",
                                  "writing": "INTERMEDIATE"
                                }
                                """))
                .andExpect(status().isCreated());
        MvcResult started = mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":9}"))
                .andExpect(status().isCreated())
                .andReturn();
        String assessmentId = JsonPath.read(started.getResponse().getContentAsString(), "$.assessmentId");

        mockMvc.perform(post("/api/v1/assessments/" + assessmentId + "/answers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemId": "initial-reading-1",
                                  "answerType": "OPTION",
                                  "option": "B",
                                  "clientDurationMs": 1500
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/assessments/" + assessmentId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.assessmentId").value(assessmentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/assessments/" + assessmentId + "/result")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(assessmentId))
                .andExpect(jsonPath("$.skills.reading.score").value(100.0000))
                .andExpect(jsonPath("$.skills.listening.evidence").isArray())
                .andExpect(jsonPath("$.priorities").isArray());
        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("RESULT"));

        MvcResult firstPlan = mockMvc.perform(get("/api/v1/plans/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").exists())
                .andExpect(jsonPath("$.totalMinutes").value(20))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks[0].status").doesNotExist())
                .andExpect(jsonPath("$.tasks[0].skillFocus").isArray())
                .andExpect(jsonPath("$.reasons").isArray())
                .andReturn();
        String planId = JsonPath.read(firstPlan.getResponse().getContentAsString(), "$.planId");
        String firstPlanFocus = JsonPath.read(firstPlan.getResponse().getContentAsString(), "$.tasks[0].skillFocus[0]");

        mockMvc.perform(get("/api/v1/plans/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId));

        MvcResult startedTraining = mockMvc.perform(post("/api/v1/training-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "training-start-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\",\"mode\":\"TEXT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.mode").value("TEXT"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();
        String trainingSessionId = JsonPath.read(startedTraining.getResponse().getContentAsString(), "$.sessionId");

        mockMvc.perform(post("/api/v1/training-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "training-start-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\",\"mode\":\"MIXED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(trainingSessionId))
                .andExpect(jsonPath("$.mode").value("TEXT"));

        MvcResult currentTrainingTask = mockMvc.perform(get("/api/v1/training-sessions/" + trainingSessionId + "/current-task")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").exists())
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andReturn();
        String trainingTaskId = JsonPath.read(currentTrainingTask.getResponse().getContentAsString(), "$.taskId");

        MvcResult submittedAttempt = mockMvc.perform(post("/api/v1/training-sessions/"
                                + trainingSessionId + "/tasks/" + trainingTaskId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "attempt-submit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputType": "TEXT",
                                  "text": "I think the delay was caused by an unstable connection.",
                                  "hintLevel": 1,
                                  "clientDurationMs": 1200
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.attemptId").exists())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.evidenceCount").value(1))
                .andReturn();
        String attemptId = JsonPath.read(submittedAttempt.getResponse().getContentAsString(), "$.attemptId");

        mockMvc.perform(post("/api/v1/training-sessions/"
                                + trainingSessionId + "/tasks/" + trainingTaskId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "attempt-submit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputType": "TEXT",
                                  "text": "I think the delay was caused by an unstable connection.",
                                  "hintLevel": 1,
                                  "clientDurationMs": 1200
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.evidenceCount").value(1));

        mockMvc.perform(post("/api/v1/training-sessions/"
                                + trainingSessionId + "/tasks/" + trainingTaskId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "attempt-submit-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputType": "AUDIO",
                                  "audioAssetId": "audio-1"
                                }
                                """))
                .andExpect(status().isBadRequest());

        MvcResult nextTrainingTask = mockMvc.perform(get("/api/v1/training-sessions/" + trainingSessionId + "/current-task")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").exists())
                .andReturn();
        String nextTrainingTaskId = JsonPath.read(nextTrainingTask.getResponse().getContentAsString(), "$.taskId");

        MvcResult streamedConversation = mockMvc.perform(post("/api/v1/conversations/"
                                + trainingSessionId + "/messages/stream")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey))
                        .header("Idempotency-Key", "conversation-stream-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "messageType": "TEXT",
                                  "text": "I very like this movie because it is exciting.",
                                  "taskId": "%s"
                                }
                                """.formatted(nextTrainingTaskId)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(streamedConversation))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:status")))
                .andExpect(content().string(containsString("event:text_delta")))
                .andExpect(content().string(containsString("event:correction_ready")))
                .andExpect(content().string(containsString("\"hasError\":true")))
                .andExpect(content().string(containsString("\"corrected\":\"really like\"")))
                .andExpect(content().string(containsString("event:done")));

        mockMvc.perform(post("/api/v1/training-sessions/" + trainingSessionId + "/pause")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mockMvc.perform(post("/api/v1/training-sessions/" + trainingSessionId + "/resume")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/v1/training-sessions/" + trainingSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("other-user")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/training-sessions/" + trainingSessionId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.dailySummary.sessionId").value(trainingSessionId))
                .andExpect(jsonPath("$.dailySummary.evidenceCount").value(1))
                .andExpect(jsonPath("$.dailySummary.practicedSkills[0]").value("speaking"));

        mockMvc.perform(post("/api/v1/training-sessions/" + trainingSessionId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"));

        MvcResult adjustedPlan = mockMvc.perform(get("/api/v1/plans/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].skillFocus[0]").value("listening"))
                .andReturn();
        String adjustedPlanId = JsonPath.read(adjustedPlan.getResponse().getContentAsString(), "$.planId");
        String adjustedPlanFocus = JsonPath.read(adjustedPlan.getResponse().getContentAsString(), "$.tasks[0].skillFocus[0]");
        assertNotEquals(planId, adjustedPlanId);
        assertNotEquals(firstPlanFocus, adjustedPlanFocus);

        mockMvc.perform(get("/api/v1/plans/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(adjustedPlanId));
    }

    @Test
    void rejectsAudioAssessmentAnswerUntilAsrExists() throws Exception {
        mockMvc.perform(post("/api/v1/assessments/assessment-1/answers")
                        .header(HttpHeaders.AUTHORIZATION, bearer("answer-type-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemId": "initial-speaking-open-1",
                                  "answerType": "AUDIO",
                                  "audioAssetId": "audio-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAssessmentStartBeforeSelfAssessment() throws Exception {
        mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer("assessment-not-ready-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAssessmentTargetOutsideBounds() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("assessment-bounds-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"GENERAL\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profile/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer("assessment-bounds-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyMinutes": 10,
                                  "correctionStyle": "LIGHT",
                                  "reminderEnabled": false,
                                  "saveRawText": true,
                                  "saveRawAudio": true
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer("assessment-bounds-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "BASIC",
                                  "speaking": "BASIC",
                                  "reading": "BASIC",
                                  "writing": "BASIC"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer("assessment-bounds-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMinutes\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsSelfAssessmentBeforePreferences() throws Exception {
        mockMvc.perform(post("/api/v1/assessments/self")
                        .header(HttpHeaders.AUTHORIZATION, bearer("not-ready-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listening": "BASIC",
                                  "speaking": "BASIC",
                                  "reading": "BASIC",
                                  "writing": "BASIC"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedPrimaryGoal() throws Exception {
        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, bearer("invalid-goal-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"TRAVEL\"}"))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String scenarioUserKey) throws Exception {
        try {
            return "Bearer " + accessTokensByScenarioUser.computeIfAbsent(scenarioUserKey, this::registerScenarioUser);
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }

    private String registerScenarioUser(String scenarioUserKey) {
        try {
            String slug = scenarioUserKey.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", ".")
                    .replaceAll("^\\.+|\\.+$", "");
            if (slug.isBlank()) {
                slug = "user";
            }
            String response = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"m9-" + slug + "@integration.test\",\"password\":\"learner-password\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            return JsonPath.read(response, "$.accessToken");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
