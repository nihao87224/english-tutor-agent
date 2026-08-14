package cn.forever24.tutor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "tutor.auth.legacy-user-key-enabled=false")
class CurrentUserKeyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void learnerEndpointsRequireBearerTokenWhenLegacyUserKeyIsDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header("X-User-Key", "legacy-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedLearnerIdentityIgnoresSpoofedLegacyHeader() throws Exception {
        AuthResult learnerA = register("saas-m2-a@example.com", "learner-password");
        AuthResult learnerB = register("saas-m2-b@example.com", "learner-password");

        mockMvc.perform(put("/api/v1/profile/primary-goal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerA.accessToken())
                        .header("X-User-Key", learnerB.userKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"WORKPLACE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryGoal").value("WORKPLACE"));

        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerB.accessToken())
                        .header("X-User-Key", learnerA.userKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("GOAL"));

        mockMvc.perform(get("/api/v1/onboarding/progress")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("PREFERENCES"));
    }

    private AuthResult register(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return new AuthResult(
                json.path("accessToken").asText(),
                json.path("user").path("userKey").asText());
    }

    private record AuthResult(String accessToken, String userKey) {
    }
}
