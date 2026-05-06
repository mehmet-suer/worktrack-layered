package com.worktrack.auth;

import com.worktrack.base.AbstractWebIntegrationTest;
import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.request.auth.RefreshTokenRequest;
import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.auth.InvalidRefreshTokenException;
import com.worktrack.repo.auth.RefreshTokenRepository;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.service.auth.AuthService;
import com.worktrack.util.JsonUtils;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.worktrack.dto.response.ErrorCode.INVALID_REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should login, refresh, rotate refresh token, and reject reuse of old token")
    void shouldRefreshAndRotateToken() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());
        assertThat(loginResponse.refreshToken()).isNotBlank();

        var refreshResult = refresh(loginResponse.refreshToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        LoginResponse refreshed = jsonUtils.fromJsonString(
                refreshResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        assertThat(refreshed.refreshToken()).isNotEqualTo(loginResponse.refreshToken());

        refresh(loginResponse.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.name()));
    }

    @Test
    @DisplayName("should logout and invalidate refresh token")
    void shouldLogoutAndInvalidateRefreshToken() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());

        logout(loginResponse.refreshToken())
                .andExpect(status().isNoContent());

        refresh(loginResponse.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.name()));
    }

    @Test
    @DisplayName("should refresh even when a malformed bearer token header is present")
    void shouldRefreshWithMalformedBearerHeader() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());

        refresh(loginResponse.refreshToken(), "Bearer definitely-not-a-jwt")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("should logout even when an expired bearer token header is present")
    void shouldLogoutWithExpiredBearerHeader() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());

        logout(loginResponse.refreshToken(), "Bearer " + expiredAccessToken(request.username(), Role.EMPLOYEE))
                .andExpect(status().isNoContent());

        refresh(loginResponse.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.name()));
    }

    @Test
    @DisplayName("should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());
        var storedToken = refreshTokenRepository.findAll().stream().findFirst().orElseThrow();
        storedToken.setExpiresAt(Instant.now().minusSeconds(5));
        refreshTokenRepository.saveAndFlush(storedToken);

        refresh(loginResponse.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.name()));
    }

    @Test
    @DisplayName("should allow only one successful concurrent refresh for the same token")
    void shouldAllowOnlyOneSuccessfulConcurrentRefresh() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<RefreshAttempt> firstAttempt = executorService.submit(
                    () -> attemptConcurrentRefresh(loginResponse.refreshToken(), ready, start)
            );
            Future<RefreshAttempt> secondAttempt = executorService.submit(
                    () -> attemptConcurrentRefresh(loginResponse.refreshToken(), ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<RefreshAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS),
                    secondAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(RefreshAttempt::successful).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.successful()).hasSize(1);
            assertThat(attempts).filteredOn(RefreshAttempt::successful)
                    .extracting(RefreshAttempt::refreshToken)
                    .doesNotContain(loginResponse.refreshToken());
            assertThat(attempts).filteredOn(attempt -> !attempt.successful())
                    .allSatisfy(attempt -> assertThat(attempt.error()).isInstanceOf(InvalidRefreshTokenException.class));
            assertThat(refreshTokenRepository.findAll()).hasSize(2);
            assertThat(refreshTokenRepository.findAll())
                    .filteredOn(token -> token.getRevokedAt() == null)
                    .hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("should reject refresh when owning user is soft-deleted")
    void shouldRejectRefreshForDeletedUser() throws Exception {
        RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
        register(request).andExpect(status().isOk());

        LoginResponse loginResponse = loginAndRead(request.username(), request.password());

        User user = userRepository.findByUsernameAndStatusNot(request.username(), Status.DELETED)
                .orElseThrow();
        user.setStatus(Status.DELETED);
        userRepository.save(user);

        refresh(loginResponse.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.name()));
    }

    @Test
    @DisplayName("should reject blank refresh token request")
    void shouldRejectBlankRefreshTokenRequest() throws Exception {
        mockMvc.perform(post("/layered/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonUtils.asJsonString(new RefreshTokenRequest(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    private ResultActions register(RegisterUserRequest request) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(request)));
    }

    private LoginResponse loginAndRead(String username, String password) throws Exception {
        var result = mockMvc.perform(post("/layered/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonUtils.asJsonString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        return jsonUtils.fromJsonString(result.getResponse().getContentAsString(), LoginResponse.class);
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return refresh(refreshToken, null);
    }

    private ResultActions refresh(String refreshToken, String authorizationHeader) throws Exception {
        var requestBuilder = post("/layered/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(new RefreshTokenRequest(refreshToken)));

        if (authorizationHeader != null) {
            requestBuilder.header("Authorization", authorizationHeader);
        }

        return mockMvc.perform(requestBuilder);
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return logout(refreshToken, null);
    }

    private ResultActions logout(String refreshToken, String authorizationHeader) throws Exception {
        var requestBuilder = post("/layered/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(new RefreshTokenRequest(refreshToken)));

        if (authorizationHeader != null) {
            requestBuilder.header("Authorization", authorizationHeader);
        }

        return mockMvc.perform(requestBuilder);
    }

    private String expiredAccessToken(String username, Role role) {
        Instant now = Instant.now();
        String header = base64Url("''{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(jsonUtils.asJsonString(Map.of(
                "sub", username,
                "role", role.name(),
                "iat", Date.from(now.minusSeconds(120)).getTime() / 1000,
                "exp", Date.from(now.minusSeconds(60)).getTime() / 1000
        )));
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    private RefreshAttempt attemptConcurrentRefresh(String refreshToken, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return RefreshAttempt.failure(new IllegalStateException("Timed out waiting to start refresh race"));
            }
            return RefreshAttempt.success(authService.refresh(refreshToken).refreshToken());
        } catch (Exception ex) {
            return RefreshAttempt.failure(ex);
        }
    }

    private record RefreshAttempt(String refreshToken, Exception error) {
        private static RefreshAttempt success(String refreshToken) {
            return new RefreshAttempt(refreshToken, null);
        }

        private static RefreshAttempt failure(Exception error) {
            return new RefreshAttempt(null, error);
        }

        private boolean successful() {
            return error == null;
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(jwtSecret), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign JWT content for test", ex);
        }
    }

    private String base64Url(String content) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}
