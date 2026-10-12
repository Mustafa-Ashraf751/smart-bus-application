package com.verysmartbus.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security filter chain. Users come from the startup seeders
 * (admin/driver/employee @verysmartbus.com, password123).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void wrongPasswordIsUnauthorizedNotServerError() throws Exception {
        mockMvc.perform(login("admin@verysmartbus.com", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void unknownEmailGetsTheSameAnswerAsWrongPassword() throws Exception {
        mockMvc.perform(login("nobody@verysmartbus.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void loginReturnsUserAndTokenCarriesFrontendClaims() throws Exception {
        String body = mockMvc.perform(login("admin@verysmartbus.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("admin@verysmartbus.com"))
                .andExpect(jsonPath("$.user.name").value("Admin User"))
                .andExpect(jsonPath("$.user.phone").value("0100000000"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        JsonNode claims = decodeClaims(response.get("accessToken").asText());

        assertThat(claims.get("sub").asText()).isEqualTo(response.at("/user/userId").asText());
        assertThat(claims.get("userId").asLong()).isEqualTo(response.at("/user/userId").asLong());
        assertThat(claims.get("email").asText()).isEqualTo("admin@verysmartbus.com");
        assertThat(claims.get("name").asText()).isEqualTo("Admin User");
        assertThat(claims.get("phone").asText()).isEqualTo("0100000000");
        assertThat(claims.has("address")).isFalse();
        assertThat(claims.get("roles").toString()).isEqualTo("[\"ADMIN\"]");
        assertThat(claims.get("permissions").toString()).contains("MANAGE_USERS", "BOOK_TRIP");
        assertThat(claims.has("iat")).isTrue();
        assertThat(claims.has("exp")).isTrue();
    }

    @Test
    void loginMatchesEmailsStoredWithCapitalLetters() throws Exception {
        if (userRepository.findByEmail("Mixed.Case@VerySmartBus.com").isEmpty()) {
            userRepository.save(AppUser.builder()
                    .name("Mixed Case")
                    .email("Mixed.Case@VerySmartBus.com")
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .build());
        }

        mockMvc.perform(login("mixed.case@verysmartbus.com", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void meRequiresTokenAndReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("driver@verysmartbus.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("driver@verysmartbus.com"))
                .andExpect(jsonPath("$.address").value("Depot"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("DRIVER"))
                .andExpect(jsonPath("$.permissions[0]").value("UPDATE_LIVE_LOCATION"));
    }

    @Test
    void actuatorMetricsAreAdminOnlyButHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, bearer("employee@verysmartbus.com")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, bearer("admin@verysmartbus.com")))
                .andExpect(status().isOk());
    }

    @Test
    void bookingRequiresBookTripPermission() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer("driver@verysmartbus.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\": 1, \"pickupTripStationId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsConfiguredOriginOnly() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(login(email, PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private JsonNode decodeClaims(String token) throws Exception {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
    }
}
