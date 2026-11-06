package com.worktrack.security.policy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationRequestPolicyTest {

    private final AuthenticationRequestPolicy policy = new AuthenticationRequestPolicy();

    @Test
    void shouldClassifyPublicRequestsByMethodAndPath() {
        assertThat(isPublic(HttpMethod.POST, "/layered/api/v1/users/register")).isTrue();
        assertThat(isPublic(HttpMethod.POST, "/layered/api/v1/auth/login")).isTrue();
        assertThat(isPublic(HttpMethod.POST, "/layered/api/v1/auth/refresh")).isTrue();
        assertThat(isPublic(HttpMethod.POST, "/layered/api/v1/auth/logout")).isTrue();
        assertThat(isPublic(HttpMethod.GET, "/actuator/health")).isTrue();
        assertThat(isPublic(HttpMethod.HEAD, "/actuator/health")).isTrue();
        assertThat(isPublic(HttpMethod.GET, "/actuator/prometheus")).isTrue();
        assertThat(isPublic(HttpMethod.HEAD, "/actuator/prometheus")).isTrue();
        assertThat(isPublic(HttpMethod.GET, "/actuator/health/liveness")).isTrue();
        assertThat(isPublic(HttpMethod.HEAD, "/actuator/health/liveness")).isTrue();
        assertThat(isPublic(HttpMethod.GET, "/actuator/health/readiness")).isTrue();
        assertThat(isPublic(HttpMethod.HEAD, "/actuator/health/readiness")).isTrue();
    }

    @Test
    void shouldKeepUnknownAndWrongMethodRequestsProtected() {
        assertThat(isPublic(HttpMethod.GET, "/layered/api/v1/auth/login")).isFalse();
        assertThat(isPublic(HttpMethod.POST, "/actuator/health")).isFalse();
        assertThat(isPublic(HttpMethod.GET, "/layered/api/v1/projects")).isFalse();
        assertThat(isPublic(HttpMethod.POST, "/layered/api/v1/auth/unknown")).isFalse();
    }

    @Test
    void shouldIgnoreAccessTokensOnlyForRefreshAndLogout() {
        assertThat(ignoresAccessToken(HttpMethod.POST, "/layered/api/v1/auth/refresh")).isTrue();
        assertThat(ignoresAccessToken(HttpMethod.POST, "/layered/api/v1/auth/logout")).isTrue();

        assertThat(ignoresAccessToken(HttpMethod.POST, "/layered/api/v1/auth/login")).isFalse();
        assertThat(ignoresAccessToken(HttpMethod.POST, "/layered/api/v1/users/register")).isFalse();
        assertThat(ignoresAccessToken(HttpMethod.GET, "/layered/api/v1/auth/refresh")).isFalse();
    }

    private boolean isPublic(HttpMethod method, String path) {
        return policy.publicRequests().matches(request(method, path));
    }

    private boolean ignoresAccessToken(HttpMethod method, String path) {
        return policy.ignoresAccessToken(request(method, path));
    }

    private MockHttpServletRequest request(HttpMethod method, String path) {
        return new MockHttpServletRequest(method.name(), path);
    }
}
