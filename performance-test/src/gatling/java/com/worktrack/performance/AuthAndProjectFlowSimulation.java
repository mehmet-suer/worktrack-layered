package com.worktrack.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class AuthAndProjectFlowSimulation extends Simulation {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private record LoginRequest(String username, String password) {
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request body to JSON", e);
        }
    }

    private static int intProperty(String name, int defaultValue) {
        String raw = System.getProperty(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid integer for -D%s: '%s'".formatted(name, raw),
                    e
            );
        }
    }

    private static double doubleProperty(String name, double defaultValue) {
        String raw = System.getProperty(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid number for -D%s: '%s'".formatted(name, raw),
                    e
            );
        }
    }

    private final String baseUrl = System.getProperty("baseUrl", "http://localhost:8000");
    private final String username = System.getProperty("perf.username", "performanceUser");
    private final String password = System.getProperty("perf.password", "performanceUser");

    private final double startUsersPerSec = doubleProperty("startUsersPerSec", 5);
    private final double usersPerSecIncrement = doubleProperty("usersPerSecIncrement", 5);
    private final int levels = intProperty("levels", 8);
    private final int levelDurationSec = intProperty("levelDurationSec", 30);
    private final int rampBetweenLevelsSec = intProperty("rampBetweenLevelsSec", 10);
    private final int businessCallsPerLogin = intProperty("businessCallsPerLogin", 5);
    private final int minThinkTimeMs = intProperty("minThinkTimeMs", 50);
    private final int maxThinkTimeMs = intProperty("maxThinkTimeMs", 250);
    private final int projectPageSize = intProperty("projectPageSize", 20);

    private final int p95LimitMs = intProperty("p95LimitMs", 1000);
    private final double maxFailedPercent = doubleProperty("maxFailedPercent", 1.0);

    private final String loginRequestBody = toJson(new LoginRequest(username, password));

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ChainBuilder login = exec(
            http("login")
                    .post("/layered/api/v1/auth/login")
                    .body(StringBody(loginRequestBody))
                    .check(status().is(200))
                    .check(jsonPath("$.token").saveAs("token"))
    ).exec(exitHereIfFailed());

    private final ChainBuilder authMe = exec(
            http("auth-me")
                    .get("/layered/api/v1/auth/me")
                    .header("Authorization", "Bearer #{token}")
                    .check(status().is(200))
    );

    private final ChainBuilder projectsForCurrentUser = exec(
            http("projects-me")
                    .get("/layered/api/v1/projects/me")
                    .header("Authorization", "Bearer #{token}")
                    .check(status().is(200))
    );

    private final ChainBuilder projectsPage = exec(
            http("projects-list")
                    .get("/layered/api/v1/projects")
                    .queryParam("size", Integer.toString(projectPageSize))
                    .header("Authorization", "Bearer #{token}")
                    .check(status().in(200, 204))
    );

    private final ChainBuilder thinkTime = pause(Duration.ofMillis(minThinkTimeMs), Duration.ofMillis(maxThinkTimeMs));

    private final ChainBuilder projectsPageWithThinkTime = projectsPage
            .pause(Duration.ofMillis(minThinkTimeMs), Duration.ofMillis(maxThinkTimeMs));

    private final ScenarioBuilder loginAndBusinessFlow =
            scenario("login-and-business-flow")
                    .exec(login)
                    .exec(thinkTime)
                    .exec(authMe)
                    .exec(thinkTime)
                    .exec(projectsForCurrentUser)
                    .exec(thinkTime)
                    .exec(repeat(businessCallsPerLogin).on(projectsPageWithThinkTime));

    {
        if (levels < 1) {
            throw new IllegalArgumentException("levels must be >= 1");
        }
        if (businessCallsPerLogin < 1) {
            throw new IllegalArgumentException("businessCallsPerLogin must be >= 1");
        }
        if (minThinkTimeMs < 0 || maxThinkTimeMs < 0 || maxThinkTimeMs < minThinkTimeMs) {
            throw new IllegalArgumentException("Think-time values are invalid");
        }

        double maxUsersPerSec = startUsersPerSec + ((levels - 1) * usersPerSecIncrement);
        int approxRequestsPerUser = businessCallsPerLogin + 3;
        double approxMaxRequestsPerSec = maxUsersPerSec * approxRequestsPerUser;

        System.out.printf(
                "Running staircase load: startUsersPerSec=%.2f, usersPerSecIncrement=%.2f, levels=%d, " +
                        "levelDurationSec=%d, rampBetweenLevelsSec=%d, businessCallsPerLogin=%d, " +
                        "thinkTimeMs=[%d,%d], approxPeakReqPerSec=%.2f%n",
                startUsersPerSec, usersPerSecIncrement, levels, levelDurationSec, rampBetweenLevelsSec,
                businessCallsPerLogin, minThinkTimeMs, maxThinkTimeMs, approxMaxRequestsPerSec
        );

        setUp(
                loginAndBusinessFlow.injectOpen(
                        incrementUsersPerSec(usersPerSecIncrement)
                                .times(levels)
                                .eachLevelLasting(Duration.ofSeconds(levelDurationSec))
                                .separatedByRampsLasting(Duration.ofSeconds(rampBetweenLevelsSec))
                                .startingFrom(startUsersPerSec)
                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().failedRequests().percent().lt(maxFailedPercent),
                        global().responseTime().percentile3().lt(p95LimitMs),
                        details("login").failedRequests().percent().lt(maxFailedPercent),
                        details("login").responseTime().percentile3().lt(p95LimitMs),
                        details("auth-me").failedRequests().percent().lt(maxFailedPercent),
                        details("projects-me").failedRequests().percent().lt(maxFailedPercent),
                        details("projects-list").failedRequests().percent().lt(maxFailedPercent),
                        details("projects-list").responseTime().percentile3().lt(p95LimitMs)
                );
    }
}
