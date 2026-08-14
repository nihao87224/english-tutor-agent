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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tutor.auth.bootstrap-admin.enabled=true",
        "tutor.auth.bootstrap-admin.email=m5-admin@example.com",
        "tutor.auth.bootstrap-admin.password=admin-password"
})
class AdminEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userReceivesForbiddenOnAdminApis() throws Exception {
        String learnerToken = register("m5-learner@example.com", "learner-password");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadDashboardAndSensitiveOperationsAreAudited() throws Exception {
        String adminToken = login("m5-admin@example.com", "admin-password");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeDefaultProvider").value("openai"));

        mockMvc.perform(put("/api/v1/admin/settings/maintenance.enabled")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": "true",
                                  "valueType": "BOOLEAN",
                                  "description": "Maintenance mode flag"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("maintenance.enabled"))
                .andExpect(jsonPath("$.value").value("true"));

        mockMvc.perform(post("/api/v1/admin/users/local-dev-user/quota/bonus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bonus\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonus").value(3));

        mockMvc.perform(get("/api/v1/admin/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.items[*].actionCode", hasItem("SYSTEM_SETTING_UPDATED")))
                .andExpect(jsonPath("$.items[*].actionCode", hasItem("USER_QUOTA_BONUS_ADDED")));
    }

    @Test
    void invalidAdminSettingTypeReturnsBadRequest() throws Exception {
        String adminToken = login("m5-admin@example.com", "admin-password");

        mockMvc.perform(put("/api/v1/admin/settings/maintenance.enabled")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": "true",
                                  "valueType": "SECRET",
                                  "description": "Maintenance mode flag"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("INVALID_ADMIN_REQUEST"));
    }

    private String register(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("accessToken").asText();
    }
}
