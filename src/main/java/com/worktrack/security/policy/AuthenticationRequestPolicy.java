package com.worktrack.security.policy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationRequestPolicy {

    private static final String USERS_REGISTER = "/layered/api/v1/users/register";
    private static final String AUTH_LOGIN = "/layered/api/v1/auth/login";
    private static final String AUTH_REFRESH = "/layered/api/v1/auth/refresh";
    private static final String AUTH_LOGOUT = "/layered/api/v1/auth/logout";
    private static final String ACTUATOR_HEALTH = "/actuator/health";
    private static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";
    private static final String ACTUATOR_LIVENESS = "/actuator/health/liveness";
    private static final String ACTUATOR_READINESS = "/actuator/health/readiness";

    private final RequestMatcher publicRequests;
    private final RequestMatcher accessTokenIgnoredRequests;

    public AuthenticationRequestPolicy() {
        var paths = PathPatternRequestMatcher.withDefaults();

        this.publicRequests = new OrRequestMatcher(
                paths.matcher(HttpMethod.POST, USERS_REGISTER),
                paths.matcher(HttpMethod.POST, AUTH_LOGIN),
                paths.matcher(HttpMethod.POST, AUTH_REFRESH),
                paths.matcher(HttpMethod.POST, AUTH_LOGOUT),
                paths.matcher(HttpMethod.GET, ACTUATOR_HEALTH),
                paths.matcher(HttpMethod.HEAD, ACTUATOR_HEALTH),
                paths.matcher(HttpMethod.GET, ACTUATOR_PROMETHEUS),
                paths.matcher(HttpMethod.HEAD, ACTUATOR_PROMETHEUS),
                paths.matcher(HttpMethod.GET, ACTUATOR_LIVENESS),
                paths.matcher(HttpMethod.HEAD, ACTUATOR_LIVENESS),
                paths.matcher(HttpMethod.GET, ACTUATOR_READINESS),
                paths.matcher(HttpMethod.HEAD, ACTUATOR_READINESS)
        );

        this.accessTokenIgnoredRequests = new OrRequestMatcher(
                paths.matcher(HttpMethod.POST, AUTH_REFRESH),
                paths.matcher(HttpMethod.POST, AUTH_LOGOUT)
        );
    }

    public RequestMatcher publicRequests() {
        return publicRequests;
    }

    public boolean ignoresAccessToken(HttpServletRequest request) {
        return accessTokenIgnoredRequests.matches(request);
    }
}
