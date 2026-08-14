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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tutor.auth.bootstrap-admin.enabled=true",
        "tutor.auth.bootstrap-admin.email=admin@example.com",
        "tutor.auth.bootstrap-admin.password=admin-password"
})
class AuthEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersUserAndReadsCurrentUserWithBearerToken() throws Exception {
        AuthResult auth = register("Learner@Example.COM", "learner-password");

        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("Learner@Example.COM"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void duplicateNormalizedEmailReturnsConflict() throws Exception {
        register("duplicate@example.com", "learner-password");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" DUPLICATE@example.com ","password":"learner-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void invalidLoginDoesNotRevealAccountExistence() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesTokenAndRejectsOldRefreshToken() throws Exception {
        AuthResult auth = register("rotate@example.com", "learner-password");

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + auth.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(readAccessToken(refreshResponse)).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + auth.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesCurrentRefreshToken() throws Exception {
        AuthResult auth = register("logout@example.com", "learner-password");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + auth.refreshToken() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + auth.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapAdminCanLoginWithAdminRole() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"admin-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode roles = objectMapper.readTree(response).path("user").path("roles");
        assertThat(roles.toString()).contains("ADMIN");
    }

    private AuthResult register(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("ETA_REFRESH_TOKEN=");
        return new AuthResult(readAccessToken(response), extractRefreshToken(setCookie));
    }

    private String readAccessToken(String response) throws Exception {
        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private static String extractRefreshToken(String setCookie) {
        return setCookie.substring("ETA_REFRESH_TOKEN=".length(), setCookie.indexOf(';'));
    }

    private record AuthResult(String accessToken, String refreshToken) {
    }
}
